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

class MessageHandler {
    private final QueueEntryList<QueueEntry> entries;
    
    public void enqueue(ServerMessage message) throws AMQException
    {
        enqueue(message, null);
    }

    public void enqueue(ServerMessage message, PostEnqueueAction action) throws AMQException
    {
        enqueue(message, false, action);
    }

    public void enqueue(ServerMessage message, boolean transactional, PostEnqueueAction action) throws AMQException
    {

        if(transactional)
        {
            incrementTxnEnqueueStats(message);
        }
        incrementQueueCount();
        incrementQueueSize(message);

        _totalMessagesReceived.incrementAndGet();


        QueueEntry entry;
        final Subscription exclusiveSub = _exclusiveSubscriber;
        entry = _entries.add(message);

        if(action != null || (exclusiveSub == null  && _queueRunner.isIdle()))
        {
            /*

            iterate over subscriptions and if any is at the end of the queue and can deliver this message, then deliver the message

             */
            SubscriptionList.SubscriptionNode node = _subscriptionList.getMarkedNode();
            SubscriptionList.SubscriptionNode nextNode = node.findNext();
            if (nextNode == null)
            {
                nextNode = _subscriptionList.getHead().findNext();
            }
            while (nextNode != null)
            {
                if (_subscriptionList.updateMarkedNode(node, nextNode))
                {
                    break;
                }
                else
                {
                    node = _subscriptionList.getMarkedNode();
                    nextNode = node.findNext();
                    if (nextNode == null)
                    {
                        nextNode = _subscriptionList.getHead().findNext();
                    }
                }
            }

            // always do one extra loop after we believe we've finished
            // this catches the case where we *just* miss an update
            int loops = 2;

            while (entry.isAvailable() && loops != 0)
            {
                if (nextNode == null)
                {
                    loops--;
                    nextNode = _subscriptionList.getHead();
                }
                else
                {
                    // if subscription at end, and active, offer
                    Subscription sub = nextNode.getSubscription();
                    deliverToSubscription(sub, entry);
                }
                nextNode = nextNode.findNext();

            }
        }


        if (entry.isAvailable())
        {
            checkSubscriptionsNotAheadOfDelivery(entry);

            if (exclusiveSub != null)
            {
                deliverAsync(exclusiveSub);
            }
            else
            {
                deliverAsync();
           }
        }

        checkForNotification(entry.getMessage());

        if(action != null)
        {
            action.onEnqueue(entry);
        }

    }

    private void deliverToSubscription(final Subscription sub, final QueueEntry entry)
            throws AMQException
    {

        if(sub.trySendLock())
        {
            try
            {
                if (!sub.isSuspended()
                    && subscriptionReadyAndHasInterest(sub, entry)
                    && mightAssign(sub, entry)
                    && !sub.wouldSuspend(entry))
                {
                    if (sub.acquires() && !(assign(sub, entry) && entry.acquire(sub)))
                    {
                        // restore credit here that would have been taken away by wouldSuspend since we didn't manage
                        // to acquire the entry for this subscription
                        sub.restoreCredit(entry);
                    }
                    else
                    {
                        deliverMessage(sub, entry, false);
                    }
                }
            }
            finally
            {
                sub.releaseSendLock();
            }
        }
    }

    private boolean assign(final Subscription sub, final QueueEntry entry)
    {
        return _messageGroupManager == null || _messageGroupManager.acceptMessage(sub, entry);
    }


    private boolean mightAssign(final Subscription sub, final QueueEntry entry)
    {
        if(_messageGroupManager == null || !sub.acquires())
        {
            return true;
        }
        Subscription assigned = _messageGroupManager.getAssignedSubscription(entry);
        return (assigned == null) || (assigned == sub);
    }

    protected void checkSubscriptionsNotAheadOfDelivery(final QueueEntry entry)
    {
        // This method is only required for queues which mess with ordering
        // Simple Queues don't :-)
    }

    private void incrementQueueSize(final ServerMessage message)
    {
        long size = message.getSize();
        getAtomicQueueSize().addAndGet(size);
        _enqueueCount.incrementAndGet();
        _enqueueSize.addAndGet(size);
        if(message.isPersistent() && isDurable())
        {
            _persistentMessageEnqueueSize.addAndGet(size);
            _persistentMessageEnqueueCount.incrementAndGet();
        }
    }

    private void updateSubRequeueEntry(final Subscription sub, final QueueEntry entry)
    {

        QueueContext subContext = (QueueContext) sub.getQueueContext();
        if(subContext != null)
        {
            QueueEntry oldEntry;

            while((oldEntry  = subContext.getReleasedEntry()) == null || oldEntry.compareTo(entry) > 0)
            {
                if(QueueContext._releasedUpdater.compareAndSet(subContext, oldEntry, entry))
                {
                    break;
                }
            }
        }
    }

    public void requeue(QueueEntry entry)
    {
        SubscriptionList.SubscriptionNodeIterator subscriberIter = _subscriptionList.iterator();
        // iterate over all the subscribers, and if they are in advance of this queue entry then move them backwards
        while (subscriberIter.advance() && entry.isAvailable())
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

    public void dequeue(QueueEntry entry, Subscription sub)
    {
        decrementQueueCount();
        decrementQueueSize(entry);
        if (entry.acquiredBySubscription())
        {
            _deliveredMessages.decrementAndGet();
        }

        if(sub != null && sub.isSessionTransactional())
        {
            incrementTxnDequeueStats(entry);
        }

        checkCapacity();

    }

    private void decrementQueueSize(final QueueEntry entry)
    {
        final ServerMessage message = entry.getMessage();
        long size = message.getSize();
        getAtomicQueueSize().addAndGet(-size);
        _dequeueSize.addAndGet(size);
        if(message.isPersistent() && isDurable())
        {
            _persistentMessageDequeueSize.addAndGet(size);
            _persistentMessageDequeueCount.incrementAndGet();
        }
    }

    void decrementQueueCount()
    {
        getAtomicQueueCount().decrementAndGet();
        _dequeueCount.incrementAndGet();
    }

    public boolean resend(final QueueEntry entry, final Subscription subscription) throws AMQException
    {
        /* TODO : This is wrong as the subscription may be suspended, we should instead change the state of the message
                  entry to resend and move back the subscription pointer. */

        subscription.getSendLock();
        try
        {
            if (!subscription.isClosed())
            {
                deliverMessage(subscription, entry, false);
                return true;
            }
            else
            {
                return false;
            }
        }
        finally
        {
            subscription.releaseSendLock();
        }
    }

    public int getConsumerCount()
    {
        return _subscriptionList.size();
    }

    public int getConsumerCountHigh()
    {
        return _counsumerCountHigh.get();
    }

    public int getActiveConsumerCount()
    {
        return _activeSubscriberCount.get();
    }

    public boolean isUnused()
    {
        return getConsumerCount() == 0;
    }

    public boolean isEmpty()
    {
        return getMessageCount() == 0;
    }

    public int getMessageCount()
    {
        return getAtomicQueueCount().get();
    }

    public long getQueueDepth()
    {
        return getAtomicQueueSize().get();
    }

    public int getUndeliveredMessageCount()
    {
        int count = getMessageCount() - _deliveredMessages.get();
        if (count < 0)
        {
            return 0;
        }
        else
        {
            return count;
        }
    }

    public long getReceivedMessageCount()
    {
        return _totalMessagesReceived.get();
    }

    public long getOldestMessageArrivalTime()
    {
        QueueEntry entry = getOldestQueueEntry();
        return entry == null ? Long.MAX_VALUE : entry.getMessage().getArrivalTime();
    }

    protected QueueEntry getOldestQueueEntry()
    {
        return _entries.next(_entries.getHead());
    }

    public List<QueueEntry> getMessagesOnTheQueue()
    {
        ArrayList<QueueEntry> entryList = new ArrayList<QueueEntry>();
        QueueEntryIterator queueListIterator = _entries.iterator();
        while (queueListIterator.advance())
        {
            QueueEntry node = queueListIterator.getNode();
            if (node != null && !node.isDispensed())
            {
                entryList.add(node);
            }
        }
        return entryList;

    }

    public void stateChange(Subscription sub, Subscription.State oldState, Subscription.State newState)
    {
        if (oldState == Subscription.State.ACTIVE && newState != Subscription.State.ACTIVE)
        {
            _activeSubscriberCount.decrementAndGet();

        }
        else if (newState == Subscription.State.ACTIVE)
        {
            if (oldState != Subscription.State.ACTIVE)
            {
                _activeSubscriberCount.incrementAndGet();

            }
            deliverAsync(sub);
        }
    }

    public AtomicInteger getAtomicQueueCount()
    {
        return _atomicQueueCount;
    }

    public AtomicLong getAtomicQueueSize()
    {
        return _atomicQueueSize;
    }

    public boolean hasExclusiveSubscriber()
    {
        return _exclusiveSubscriber != null;
    }

    private void setExclusiveSubscriber(Subscription exclusiveSubscriber)
    {
        _exclusiveSubscriber = exclusiveSubscriber;
    }

    long getStateChangeCount()
    {
        return _stateChangeCount.get();
    }

    /** Used to track bindings to exchanges so that on deletion they can easily be cancelled. */
    protected QueueEntryList getEntries()
    {
        return _entries;
    }

    protected SubscriptionList getSubscriptionList()
    {
        return _subscriptionList;
    }

    public List<QueueEntry> getMessagesOnTheQueue(final long fromMessageId, final long toMessageId)
    {
        return getMessagesOnTheQueue(new QueueEntryFilter()
        {

            public boolean accept(QueueEntry entry)
            {
                final long messageId = entry.getMessage().getMessageNumber();
                return messageId >= fromMessageId && messageId <= toMessageId;
            }

            public boolean filterComplete()
            {
                return false;
            }
        });
    }

    public QueueEntry getMessageOnTheQueue(final long messageId)
    {
        List<QueueEntry> entries = getMessagesOnTheQueue(new QueueEntryFilter()
        {
            private boolean _complete;

            public boolean accept(QueueEntry entry)
            {
                _complete = entry.getMessage().getMessageNumber() == messageId;
                return _complete;
            }

            public boolean filterComplete()
            {
                return _complete;
            }
        });
        return entries.isEmpty() ? null : entries.get(0);
    }

    public List<QueueEntry> getMessagesOnTheQueue(QueueEntryFilter filter)
    {
        ArrayList<QueueEntry> entryList = new ArrayList<QueueEntry>();
        QueueEntryIterator queueListIterator = _entries.iterator();
        while (queueListIterator.advance() && !filter.filterComplete())
        {
            QueueEntry node = queueListIterator.getNode();
            if (!node.isDispensed() && filter.accept(node))
            {
                entryList.add(node);
            }
        }
        return entryList;

    }

    public void visit(final QueueEntryVisitor visitor)
    {
        QueueEntryIterator queueListIterator = _entries.iterator();

        while(queueListIterator.advance())
        {
            QueueEntry node = queueListIterator.getNode();

            if(!node.isDispensed())
            {
                if(visitor.visit(node))
                {
                    break;
                }
            }
        }
    }
    
    public List<QueueEntry> getMessagesRangeOnTheQueue(final long fromPosition, final long toPosition)
    {
        return getMessagesOnTheQueue(new QueueEntryFilter()
                                        {
                                            private long position = 0;

                                            public boolean accept(QueueEntry entry)
                                            {
                                                position++;
                                                return (position >= fromPosition) && (position <= toPosition);
                                            }

                                            public boolean filterComplete()
                                            {
                                                return position >= toPosition;
                                            }
                                        });

    }

    public void purge(final long request) throws AMQException
    {
        clear(request);
    }
    
    public void deleteMessageFromTop()
    {
        QueueEntryIterator queueListIterator = _entries.iterator();
        boolean noDeletes = true;

        while (noDeletes && queueListIterator.advance())
        {
            QueueEntry node = queueListIterator.getNode();
            if (node.acquire())
            {
                dequeueEntry(node);
                noDeletes = false;
            }

        }
    }

    public long clearQueue() throws AMQException
    {
        return clear(0l);
    }

    private long clear(final long request) throws AMQSecurityException
    {
        //Perform ACLs
        if (!getVirtualHost().getSecurityManager().authorisePurge(this))
        {
            throw new AMQSecurityException("Permission denied: queue " + getName());
        }

        QueueEntryIterator queueListIterator = _entries.iterator();
        long count = 0;

        ServerTransaction txn = new LocalTransaction(getVirtualHost().getMessageStore());

        while (queueListIterator.advance())
        {
            QueueEntry node = queueListIterator.getNode();
            if (node.acquire())
            {
                dequeueEntry(node, txn);
                if(++count == request)
                {
                    break;
                }
            }

        }

        txn.commit();

        return count;
    }

    private void dequeueEntry(final QueueEntry node)
    {
        ServerTransaction txn = new AutoCommitTransaction(getVirtualHost().getMessageStore());
        dequeueEntry(node, txn);
    }

    private void dequeueEntry(final QueueEntry node, ServerTransaction txn)
    {
        txn.dequeue(this, node.getMessage(),
                    new ServerTransaction.Action()
                    {

                        public void postCommit()
                        {
                            node.discard();
                        }

                        public void onRollback()
                        {

                        }
                    });
    }

    public long processQueue(QueueRunner runner) throws AMQException
    {
        long stateChangeCount = Long.MIN_VALUE;
        long previousStateChangeCount = Long.MIN_VALUE;
        long rVal = Long.MIN_VALUE;
        boolean deliveryIncomplete = true;

        boolean lastLoop = false;
        int iterations = MAX_ASYNC_DELIVERIES;

        final int numSubs = _subscriptionList.size();

        final int perSub = Math.max(iterations / Math.max(numSubs,1), 1);

        // For every message enqueue/requeue the we fire deliveryAsync() which
        // increases _stateChangeCount. If _sCC changes whilst we are in our loop
        // (detected by setting previousStateChangeCount to stateChangeCount in the loop body)
        // then we will continue to run for a maximum of iterations.
        // So whilst delivery/rejection is going on a processQueue thread will be running
        while (iterations != 0 && ((previousStateChangeCount != (stateChangeCount = _stateChangeCount.get())) || deliveryIncomplete))
        {
            // we want to have one extra loop after every subscription has reached the point where it cannot move
            // further, just in case the advance of one subscription in the last loop allows a different subscription to
            // move forward in the next iteration

            if (previousStateChangeCount != stateChangeCount)
            {
                //further asynchronous delivery is required since the
                //previous loop. keep going if iteration slicing allows.
                lastLoop = false;
                rVal = stateChangeCount;
            }

            previousStateChangeCount = stateChangeCount;
            boolean allSubscriptionsDone = true;
            boolean subscriptionDone;

            SubscriptionList.SubscriptionNodeIterator subscriptionIter = _subscriptionList.iterator();
            //iterate over the subscribers and try to advance their pointer
            while (subscriptionIter.advance())
            {
                Subscription sub = subscriptionIter.getNode().getSubscription();
                sub.getSendLock();

                    try
                    {
                        for(int i = 0 ; i < perSub; i++)
                        {
                            //attempt delivery. returns true if no further delivery currently possible to this sub
                            subscriptionDone = attemptDelivery(sub, true);
                            if (subscriptionDone)
                            {
                                sub.flushBatched();
                                if (lastLoop && !sub.isSuspended())
                                {
                                    sub.queueEmpty();
                                }
                                break;
                            }
                            else
                            {
                                //this subscription can accept additional deliveries, so we must
                                //keep going after this (if iteration slicing allows it)
                                allSubscriptionsDone = false;
                                lastLoop = false;
                                if(--iterations == 0)
                                {
                                    sub.flushBatched();
                                    break;
                                }
                            }

                        }

                        sub.flushBatched();
                    }
                    finally
                    {
                        sub.releaseSendLock();
                    }
            }

            if(allSubscriptionsDone && lastLoop)
            {
                //We have done an extra loop already and there are again
                //again no further delivery attempts possible, only
                //keep going if state change demands it.
                deliveryIncomplete = false;
            }
            else if(allSubscriptionsDone)
            {
                //All subscriptions reported being done, but we have to do
                //an extra loop if the iterations are not exhausted and
                //there is still any work to be done
                deliveryIncomplete = _subscriptionList.size() != 0;
                lastLoop = true;
            }
            else
            {
                //some subscriptions can still accept more messages,
                //keep going if iteration count allows.
                lastLoop = false;
                deliveryIncomplete = true;
            }

        }

        // If iterations == 0 then the limiting factor was the time-slicing rather than available messages or credit
        // therefore we should schedule this runner again (unless someone beats us to it :-) ).
        if (iterations == 0)
        {
            if (_logger.isDebugEnabled())
            {
                _logger.debug("Rescheduling runner:" + runner);
            }
            return 0L;
        }
        return rVal;

    }

    public void checkMessageStatus() throws AMQException
    {
        QueueEntryIterator queueListIterator = _entries.iterator();

        while (queueListIterator.advance())
        {
            QueueEntry node = queueListIterator.getNode();
            // Only process nodes that are not currently deleted and not dequeued
            if (!node.isDispensed())
            {
                // If the node has exired then acquire it
                if (node.expired() && node.acquire())
                {
                    if (_logger.isDebugEnabled())
                    {
                        _logger.debug("Dequeuing expired node " + node);
                    }
                    // Then dequeue it.
                    dequeueEntry(node);
                }
                else
                {
                    // There is a chance that the node could be deleted by
                    // the time the check actually occurs. So verify we
                    // can actually get the message to perform the check.
                    ServerMessage msg = node.getMessage();
                    if (msg != null)
                    {
                        checkForNotification(msg);
                    }
                }
            }
        }

    }

    public long getUnackedMessageCountHigh()
    {
        return _unackedMsgCountHigh.get();
    }

    public long getUnackedMessageCount()
    {
        return _unackedMsgCount.get();
    }

    public long getUnackedMessageBytes()
    {
        return _unackedMsgBytes.get();
    }

    public void decrementUnackedMsgCount(QueueEntry queueEntry)
    {
        _unackedMsgCount.decrementAndGet();
        _unackedMsgBytes.addAndGet(-queueEntry.getSize());
    }

    private void incrementUnackedMsgCount(QueueEntry entry)
    {
        long unackedMsgCount = _unackedMsgCount.incrementAndGet();
        _unackedMsgBytes.addAndGet(entry.getSize());

        long unackedMsgCountHigh;
        while(unackedMsgCount > (unackedMsgCountHigh = _unackedMsgCountHigh.get()))
        {
            if(_unackedMsgCountHigh.compareAndSet(unackedMsgCountHigh, unackedMsgCount))
            {
                break;
            }
        }
    }

    public int getMaximumDeliveryCount()
    {
        return _maximumDeliveryCount;
    }

    public void setMaximumDeliveryCount(final int maximumDeliveryCount)
    {
        _maximumDeliveryCount = maximumDeliveryCount;
    }
}