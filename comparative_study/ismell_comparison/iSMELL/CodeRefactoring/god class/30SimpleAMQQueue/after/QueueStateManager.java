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

class QueueStateManager {
    private final AtomicLong totalMessagesReceived = new AtomicLong();

    public boolean getDeleteOnNoConsumers()
    {
        return _deleteOnNoConsumers;
    }

    public void setDeleteOnNoConsumers(boolean b)
    {
        _deleteOnNoConsumers = b;
    }

    public void addBinding(final Binding binding)
    {
        _bindings.add(binding);
        int bindingCount = _bindings.size();
        int bindingCountHigh;
        while(bindingCount > (bindingCountHigh = _bindingCountHigh.get()))
        {
            if(_bindingCountHigh.compareAndSet(bindingCountHigh, bindingCount))
            {
                break;
            }
        }

        reconfigure();
    }

    private void reconfigure()
    {
        //Reconfigure the queue for to reflect this new binding.
        ConfigurationPlugin config = getVirtualHost().getConfiguration().getQueueConfiguration(this);

        if (config != null)
        {
            if (_logger.isDebugEnabled())
            {
                _logger.debug("Reconfiguring queue(" + this + ") with config:" + config + " was "+ _queueConfiguration);
            }
            // Reconfigure with new config.
            configure(config);
        }
    }

    public int getBindingCountHigh()
    {
        return _bindingCountHigh.get();
    }

    public void removeBinding(final Binding binding)
    {
        _bindings.remove(binding);

        reconfigure();
    }

    public List<Binding> getBindings()
    {
        return Collections.unmodifiableList(_bindings);
    }

    public int getBindingCount()
    {
        return getBindings().size();
    }

    public boolean isDeleted()
    {
        return _deleted.get();
    }

    public void addQueueDeleteTask(final Task task)
    {
        _deleteTaskList.add(task);
    }

    public void removeQueueDeleteTask(final Task task)
    {
        _deleteTaskList.remove(task);
    }

    public int delete() throws AMQSecurityException, AMQException
    {
        // Check access
        if (!_virtualHost.getSecurityManager().authoriseDelete(this))
        {
            throw new AMQSecurityException("Permission denied: " + getName());
        }

        if (!_deleted.getAndSet(true))
        {

            for (Binding b : getBindings())
            {
                _virtualHost.getBindingFactory().removeBinding(b);
            }

            SubscriptionList.SubscriptionNodeIterator subscriptionIter = _subscriptionList.iterator();

            while (subscriptionIter.advance())
            {
                Subscription s = subscriptionIter.getNode().getSubscription();
                if (s != null)
                {
                    s.queueDeleted(this);
                }
            }

            _virtualHost.getQueueRegistry().unregisterQueue(_name);
            getConfigStore().removeConfiguredObject(this);

            List<QueueEntry> entries = getMessagesOnTheQueue(new QueueEntryFilter()
            {

                public boolean accept(QueueEntry entry)
                {
                    return entry.acquire();
                }

                public boolean filterComplete()
                {
                    return false;
                }
            });

            ServerTransaction txn = new LocalTransaction(getVirtualHost().getMessageStore());

            if(_alternateExchange != null)
            {

                InboundMessageAdapter adapter = new InboundMessageAdapter();
                for(final QueueEntry entry : entries)
                {
                    adapter.setEntry(entry);
                    List<? extends BaseQueue> queues = _alternateExchange.route(adapter);
                    if((queues == null || queues.size() == 0) && _alternateExchange.getAlternateExchange() != null)
                    {
                        queues = _alternateExchange.getAlternateExchange().route(adapter);
                    }

                    final ServerMessage message = entry.getMessage();
                    if(queues != null && queues.size() != 0)
                    {
                        final List<? extends BaseQueue> rerouteQueues = queues;
                        txn.enqueue(rerouteQueues, entry.getMessage(),
                                    new ServerTransaction.Action()
                                    {

                                        public void postCommit()
                                        {
                                            try
                                            {
                                                for(BaseQueue queue : rerouteQueues)
                                                {
                                                    queue.enqueue(message);
                                                }
                                            }
                                            catch (AMQException e)
                                            {
                                                throw new RuntimeException(e);
                                            }

                                        }

                                        public void onRollback()
                                        {

                                        }
                                    }, 0L);
                        txn.dequeue(this, entry.getMessage(),
                                    new ServerTransaction.Action()
                                    {

                                        public void postCommit()
                                        {
                                            entry.discard();
                                        }

                                        public void onRollback()
                                        {
                                        }
                                    });
                    }

                }

                _alternateExchange.removeReference(this);
            }
            else
            {
                // TODO log discard

                for(final QueueEntry entry : entries)
                {
                    final ServerMessage message = entry.getMessage();
                    if(message != null)
                    {
                        txn.dequeue(this, message,
                                    new ServerTransaction.Action()
                                    {

                                        public void postCommit()
                                        {
                                            entry.discard();
                                        }

                                        public void onRollback()
                                        {
                                        }
                                    });
                    }
                }
            }

            txn.commit();

            for (Task task : _deleteTaskList)
            {
                task.doTask(this);
            }

            _deleteTaskList.clear();
            stop();

            //Log Queue Deletion
            CurrentActor.get().message(_logSubject, QueueMessages.DELETED());

        }
        return getMessageCount();

    }

    public void stop()
    {
        if (!_stopped.getAndSet(true))
        {
            ReferenceCountingExecutorService.getInstance().releaseExecutorService();
        }
    }

    public void checkCapacity(AMQSessionModel channel)
    {
        if(_capacity != 0l)
        {
            if(_atomicQueueSize.get() > _capacity)
            {
                _overfull.set(true);
                //Overfull log message
                _logActor.message(_logSubject, QueueMessages.OVERFULL(_atomicQueueSize.get(), _capacity));

                _blockedChannels.add(channel);

                channel.block(this);

                if(_atomicQueueSize.get() <= _flowResumeCapacity)
                {

                    //Underfull log message
                    _logActor.message(_logSubject, QueueMessages.UNDERFULL(_atomicQueueSize.get(), _flowResumeCapacity));

                   channel.unblock(this);
                   _blockedChannels.remove(channel);

                }

            }



        }
    }

    private void checkCapacity()
    {
        if(_capacity != 0L)
        {
            if(_overfull.get() && _atomicQueueSize.get() <= _flowResumeCapacity)
            {
                if(_overfull.compareAndSet(true,false))
                {//Underfull log message
                    _logActor.message(_logSubject, QueueMessages.UNDERFULL(_atomicQueueSize.get(), _flowResumeCapacity));
                }

                for(final AMQSessionModel blockedChannel : _blockedChannels)
                {
                    blockedChannel.unblock(this);
                    _blockedChannels.remove(blockedChannel);
                }
            }
        }
    }

    public boolean isOverfull()
    {
        return _overfull.get();
    }

    public Set<NotificationCheck> getNotificationChecks()
    {
        return _notificationChecks;
    }
}