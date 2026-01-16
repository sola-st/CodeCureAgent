package org.apache.qpid.server.queue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.apache.qpid.AMQException;
import org.apache.qpid.AMQSecurityException;
import org.apache.qpid.framing.AMQShortString;
import org.apache.qpid.pool.ReferenceCountingExecutorService;
import org.apache.qpid.server.binding.Binding;
import org.apache.qpid.server.configuration.ConfigStore;
import org.apache.qpid.server.configuration.ConfiguredObject;
import org.apache.qpid.server.configuration.QueueConfigType;
import org.apache.qpid.server.configuration.QueueConfiguration;
import org.apache.qpid.server.configuration.plugins.ConfigurationPlugin;
import org.apache.qpid.server.exchange.Exchange;
import org.apache.qpid.server.logging.LogActor;
import org.apache.qpid.server.logging.LogSubject;
import org.apache.qpid.server.logging.actors.CurrentActor;
import org.apache.qpid.server.logging.actors.QueueActor;
import org.apache.qpid.server.logging.messages.QueueMessages;
import org.apache.qpid.server.logging.subjects.QueueLogSubject;
import org.apache.qpid.server.message.ServerMessage;
import org.apache.qpid.server.protocol.AMQSessionModel;
import org.apache.qpid.server.registry.ApplicationRegistry;
import org.apache.qpid.server.security.AuthorizationHolder;
import org.apache.qpid.server.subscription.AssignedSubscriptionMessageGroupManager;
import org.apache.qpid.server.subscription.DefinedGroupMessageGroupManager;
import org.apache.qpid.server.subscription.MessageGroupManager;
import org.apache.qpid.server.subscription.Subscription;
import org.apache.qpid.server.subscription.SubscriptionList;
import org.apache.qpid.server.txn.AutoCommitTransaction;
import org.apache.qpid.server.txn.LocalTransaction;
import org.apache.qpid.server.txn.ServerTransaction;
import org.apache.qpid.server.virtualhost.VirtualHost;

class SubscriptionManager {
    private final SubscriptionList subscriptionList = new SubscriptionList();
    private final List<SubscriptionRegistrationListener> subscriptionListeners = new ArrayList<>();
    
    public synchronized void registerSubscription(final Subscription subscription, final boolean exclusive)
            throws AMQSecurityException, ExistingExclusiveSubscription, ExistingSubscriptionPreventsExclusive
    {
        // Access control
        if (!getVirtualHost().getSecurityManager().authoriseConsume(this))
        {
            throw new AMQSecurityException("Permission denied");
        }


        if (hasExclusiveSubscriber())
        {
            throw new ExistingExclusiveSubscription();
        }

        if (exclusive && !subscription.isTransient())
        {
            if (getConsumerCount() != 0)
            {
                throw new ExistingSubscriptionPreventsExclusive();
            }
            else
            {
                _exclusiveSubscriber = subscription;
            }
        }

        if(subscription.isActive())
        {
            _activeSubscriberCount.incrementAndGet();
        }
        subscription.setStateListener(this);
        subscription.setQueueContext(new QueueContext(_entries.getHead()));

        if (!isDeleted())
        {
            subscription.setQueue(this, exclusive);
            if(_nolocal)
            {
                subscription.setNoLocal(_nolocal);
            }

            synchronized (_subscriptionListeners)
            {
                for(SubscriptionRegistrationListener listener : _subscriptionListeners)
                {
                    listener.subscriptionRegistered(this, subscription);
                }
            }

            _subscriptionList.add(subscription);

            //Increment consumerCountHigh if necessary. (un)registerSubscription are both
            //synchronized methods so we don't need additional synchronization here
            if(_counsumerCountHigh.get() < getConsumerCount())
            {
                _counsumerCountHigh.incrementAndGet();
            }

            if (isDeleted())
            {
                subscription.queueDeleted(this);
            }
        }
        else
        {
            // TODO
        }

        deliverAsync(subscription);

    }

    public synchronized void unregisterSubscription(final Subscription subscription) throws AMQException
    {
        if (subscription == null)
        {
            throw new NullPointerException("subscription argument is null");
        }

        boolean removed = _subscriptionList.remove(subscription);

        if (removed)
        {
            subscription.close();
            // No longer can the queue have an exclusive consumer
            setExclusiveSubscriber(null);
            subscription.setQueueContext(null);

            if(_messageGroupManager != null)
            {
                resetSubPointersForGroups(subscription, true);
            }

            synchronized (_subscriptionListeners)
            {
                for(SubscriptionRegistrationListener listener : _subscriptionListeners)
                {
                    listener.subscriptionUnregistered(this, subscription);
                }
            }

            // auto-delete queues must be deleted if there are no remaining subscribers

            if (_autoDelete && getDeleteOnNoConsumers() && !subscription.isTransient() && getConsumerCount() == 0  )
            {
                if (_logger.isInfoEnabled())
                {
                    _logger.info("Auto-deleteing queue:" + this);
                }

                delete();

                // we need to manually fire the event to the removed subscription (which was the last one left for this
                // queue. This is because the delete method uses the subscription set which has just been cleared
                subscription.queueDeleted(this);
            }
        }
    }

    public Collection<Subscription> getConsumers()
    {
        List<Subscription> consumers = new ArrayList<Subscription>();
        SubscriptionList.SubscriptionNodeIterator iter = _subscriptionList.iterator();
        while(iter.advance())
        {
            consumers.add(iter.getNode().getSubscription());
        }
        return consumers;

    }

    public void addSubscriptionRegistrationListener(final SubscriptionRegistrationListener listener)
    {
        synchronized (_subscriptionListeners)
        {
            _subscriptionListeners.add(listener);
        }
    }

    public void removeSubscriptionRegistrationListener(final SubscriptionRegistrationListener listener)
    {
        synchronized (_subscriptionListeners)
        {
            _subscriptionListeners.remove(listener);
        }
    }

    public void resetSubPointersForGroups(Subscription subscription, boolean clearAssignments)
    {
        QueueEntry entry = _messageGroupManager.findEarliestAssignedAvailableEntry(subscription);
        if(clearAssignments)
        {
            _messageGroupManager.clearAssignments(subscription);
        }

        if(entry != null)
        {
            SubscriptionList.SubscriptionNodeIterator subscriberIter = _subscriptionList.iterator();
            // iterate over all the subscribers, and if they are in advance of this queue entry then move them backwards
            while (subscriberIter.advance())
            {
                Subscription sub = subscriberIter.getNode().getSubscription();

                // we don't make browsers send the same stuff twice
                if (sub.seesRequeues())
                {
                    updateSubRequeueEntry(sub, entry);
                }
            }

            deliverAsync();

        }
    }
}