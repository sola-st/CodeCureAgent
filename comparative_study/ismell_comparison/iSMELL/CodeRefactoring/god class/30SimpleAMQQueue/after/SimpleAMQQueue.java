/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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

public class SimpleAMQQueue implements AMQQueue, Subscription.StateListener, MessageGroupManager.SubscriptionResetHelper
{
    private static final Logger _logger = Logger.getLogger(SimpleAMQQueue.class);

    private static final String QPID_GROUP_HEADER_KEY = "qpid.group_header_key";
    private static final String QPID_SHARED_MSG_GROUP = "qpid.shared_msg_group";
    private static final String QPID_DEFAULT_MESSAGE_GROUP = "qpid.default-message-group";
    private static final String QPID_NO_GROUP = "qpid.no-group";
    // TODO - should make this configurable at the vhost / broker level
    private static final int DEFAULT_MAX_GROUPS = 255;

    private final VirtualHost _virtualHost;

    private final AMQShortString _name;

    /** null means shared */
    private final AMQShortString _owner;

    private AuthorizationHolder _authorizationHolder;

    private boolean _exclusive = false;
    private AMQSessionModel _exclusiveOwner;


    private final boolean _durable;

    /** If true, this queue is deleted when the last subscriber is removed */
    private final boolean _autoDelete;

    private Exchange _alternateExchange;


    private final QueueEntryList<QueueEntry> _entries;

    private final SubscriptionList _subscriptionList = new SubscriptionList();

    private volatile Subscription _exclusiveSubscriber;



    private final AtomicInteger _atomicQueueCount = new AtomicInteger(0);

    private final AtomicLong _atomicQueueSize = new AtomicLong(0L);

    private final AtomicInteger _activeSubscriberCount = new AtomicInteger();

    private final AtomicLong _totalMessagesReceived = new AtomicLong();

    private final AtomicLong _dequeueCount = new AtomicLong();
    private final AtomicLong _dequeueSize = new AtomicLong();
    private final AtomicLong _enqueueCount = new AtomicLong();
    private final AtomicLong _enqueueSize = new AtomicLong();
    private final AtomicLong _persistentMessageEnqueueSize = new AtomicLong();
    private final AtomicLong _persistentMessageDequeueSize = new AtomicLong();
    private final AtomicLong _persistentMessageEnqueueCount = new AtomicLong();
    private final AtomicLong _persistentMessageDequeueCount = new AtomicLong();
    private final AtomicInteger _counsumerCountHigh = new AtomicInteger(0);
    private final AtomicLong _msgTxnEnqueues = new AtomicLong(0);
    private final AtomicLong _byteTxnEnqueues = new AtomicLong(0);
    private final AtomicLong _msgTxnDequeues = new AtomicLong(0);
    private final AtomicLong _byteTxnDequeues = new AtomicLong(0);
    private final AtomicLong _unackedMsgCount = new AtomicLong(0);
    private final AtomicLong _unackedMsgCountHigh = new AtomicLong(0);
    private final AtomicLong _unackedMsgBytes = new AtomicLong();

    private final AtomicInteger _bindingCountHigh = new AtomicInteger();

    /** max allowed size(KB) of a single message */
    private long _maximumMessageSize = ApplicationRegistry.getInstance().getConfiguration().getMaximumMessageSize();

    /** max allowed number of messages on a queue. */
    private long _maximumMessageCount = ApplicationRegistry.getInstance().getConfiguration().getMaximumMessageCount();

    /** max queue depth for the queue */
    private long _maximumQueueDepth = ApplicationRegistry.getInstance().getConfiguration().getMaximumQueueDepth();

    /** maximum message age before alerts occur */
    private long _maximumMessageAge = ApplicationRegistry.getInstance().getConfiguration().getMaximumMessageAge();

    /** the minimum interval between sending out consecutive alerts of the same type */
    private long _minimumAlertRepeatGap = ApplicationRegistry.getInstance().getConfiguration().getMinimumAlertRepeatGap();

    private long _capacity = ApplicationRegistry.getInstance().getConfiguration().getCapacity();

    private long _flowResumeCapacity = ApplicationRegistry.getInstance().getConfiguration().getFlowResumeCapacity();

    private final Set<NotificationCheck> _notificationChecks = EnumSet.noneOf(NotificationCheck.class);


    static final int MAX_ASYNC_DELIVERIES = 80;


    private final AtomicLong _stateChangeCount = new AtomicLong(Long.MIN_VALUE);

    private final Executor _asyncDelivery;
    private AtomicInteger _deliveredMessages = new AtomicInteger();
    private AtomicBoolean _stopped = new AtomicBoolean(false);

    private final Set<AMQSessionModel> _blockedChannels = new ConcurrentSkipListSet<AMQSessionModel>();

    private final AtomicBoolean _deleted = new AtomicBoolean(false);
    private final List<Task> _deleteTaskList = new CopyOnWriteArrayList<Task>();


    private LogSubject _logSubject;
    private LogActor _logActor;

    private static final String SUB_FLUSH_RUNNER = "SUB_FLUSH_RUNNER";
    private boolean _nolocal;

    private final AtomicBoolean _overfull = new AtomicBoolean(false);
    private boolean _deleteOnNoConsumers;
    private final CopyOnWriteArrayList<Binding> _bindings = new CopyOnWriteArrayList<Binding>();
    private UUID _id;
    private final Map<String, Object> _arguments;

    //TODO : persist creation time
    private long _createTime = System.currentTimeMillis();
    private UUID _qmfId;
    private ConfigurationPlugin _queueConfiguration;

    /** the maximum delivery count for each message on this queue or 0 if maximum delivery count is not to be enforced. */
    private int _maximumDeliveryCount = ApplicationRegistry.getInstance().getConfiguration().getMaxDeliveryCount();
    private final MessageGroupManager _messageGroupManager;

    private final Collection<SubscriptionRegistrationListener> _subscriptionListeners =
            new ArrayList<SubscriptionRegistrationListener>();

    private AMQQueue.NotificationListener _notificationListener;
    private final long[] _lastNotificationTimes = new long[NotificationCheck.values().length];

    protected SimpleAMQQueue(UUID id, AMQShortString name, boolean durable, AMQShortString owner, boolean autoDelete, boolean exclusive, VirtualHost virtualHost, Map<String,Object> arguments)
    {
        this(id, name, durable, owner, autoDelete, exclusive,virtualHost, new SimpleQueueEntryList.Factory(), arguments);
    }

    public SimpleAMQQueue(UUID id, String queueName, boolean durable, String owner, boolean autoDelete, boolean exclusive, VirtualHost virtualHost, Map<String, Object> arguments)
    {
        this(id, queueName, durable, owner, autoDelete, exclusive, virtualHost, new SimpleQueueEntryList.Factory(), arguments);
    }

    public SimpleAMQQueue(UUID id, String queueName, boolean durable, String owner, boolean autoDelete, boolean exclusive, VirtualHost virtualHost, QueueEntryListFactory entryListFactory, Map<String, Object> arguments)
    {
        this(id, queueName == null ? null : new AMQShortString(queueName), durable, owner == null ? null : new AMQShortString(owner), autoDelete, exclusive, virtualHost, entryListFactory, arguments);
    }

    protected SimpleAMQQueue(UUID id,
                             AMQShortString name,
                             boolean durable,
                             AMQShortString owner,
                             boolean autoDelete,
                             boolean exclusive,
                             VirtualHost virtualHost,
                             QueueEntryListFactory entryListFactory, Map<String,Object> arguments)
    {

        if (name == null)
        {
            throw new IllegalArgumentException("Queue name must not be null");
        }

        if (virtualHost == null)
        {
            throw new IllegalArgumentException("Virtual Host must not be null");
        }

        _name = name;
        _durable = durable;
        _owner = owner;
        _autoDelete = autoDelete;
        _exclusive = exclusive;
        _virtualHost = virtualHost;
        _entries = entryListFactory.createQueueEntryList(this);
        _arguments = arguments == null ? new HashMap<String, Object>() : new HashMap<String, Object>(arguments);

        _id = id;
        _qmfId = getConfigStore().createId();
        _asyncDelivery = ReferenceCountingExecutorService.getInstance().acquireExecutorService();

        _logSubject = new QueueLogSubject(this);
        _logActor = new QueueActor(this, CurrentActor.get().getRootMessageLogger());

        // Log the creation of this Queue.
        // The priorities display is toggled on if we set priorities > 0
        CurrentActor.get().message(_logSubject,
                                   QueueMessages.CREATED(String.valueOf(_owner),
                                                         _entries.getPriorities(),
                                                         _owner != null,
                                                         autoDelete,
                                                         durable, !durable,
                                                         _entries.getPriorities() > 0));

        getConfigStore().addConfiguredObject(this);

        if(arguments != null && arguments.containsKey(QPID_GROUP_HEADER_KEY))
        {
            if(arguments.containsKey(QPID_SHARED_MSG_GROUP) && String.valueOf(arguments.get(QPID_SHARED_MSG_GROUP)).equals("1"))
            {
                Object defaultGroup = arguments.get(QPID_DEFAULT_MESSAGE_GROUP);
                _messageGroupManager =
                        new DefinedGroupMessageGroupManager(String.valueOf(arguments.get(QPID_GROUP_HEADER_KEY)),
                                defaultGroup == null ? QPID_NO_GROUP : defaultGroup.toString(),
                                this);
            }
            else
            {
                _messageGroupManager = new AssignedSubscriptionMessageGroupManager(String.valueOf(arguments.get(QPID_GROUP_HEADER_KEY)), DEFAULT_MAX_GROUPS);
            }
        }
        else
        {
            _messageGroupManager = null;
        }

        resetNotifications();

    }

    public void resetNotifications()
    {
        // This ensure that the notification checks for the configured alerts are created.
        setMaximumMessageAge(_maximumMessageAge);
        setMaximumMessageCount(_maximumMessageCount);
        setMaximumMessageSize(_maximumMessageSize);
        setMaximumQueueDepth(_maximumQueueDepth);
    }

    // ------ Getters and Setters

    public void execute(Runnable runnable)
    {
        try
        {
            _asyncDelivery.execute(runnable);
        }
        catch (RejectedExecutionException ree)
        {
            if (_stopped.get())
            {
                // Ignore - SubFlusherRunner or QueueRunner submitted execution as queue was being stopped.
            }
            else
            {
                _logger.error("Unexpected rejected execution", ree);
                throw ree;
            }
        }
    }

    public AMQShortString getNameShortString()
    {
        return _name;
    }

    public void setNoLocal(boolean nolocal)
    {
        _nolocal = nolocal;
    }

    public UUID getId()
    {
        return _id;
    }

    @Override
    public UUID getQMFId()
    {
        return _qmfId;
    }

    public QueueConfigType getConfigType()
    {
        return QueueConfigType.getInstance();
    }

    public ConfiguredObject getParent()
    {
        return getVirtualHost();
    }

    public boolean isDurable()
    {
        return _durable;
    }

    public boolean isExclusive()
    {
        return _exclusive;
    }

    public void setExclusive(boolean exclusive)
    {
        _exclusive = exclusive;
    }

    public Exchange getAlternateExchange()
    {
        return _alternateExchange;
    }

    public void setAlternateExchange(Exchange exchange)
    {
        if(_alternateExchange != null)
        {
            _alternateExchange.removeReference(this);
        }
        if(exchange != null)
        {
            exchange.addReference(this);
        }
        _alternateExchange = exchange;
    }

    /**
     * Arguments used to create this queue.  The caller is assured
     * that null will never be returned.
     */
    public Map<String, Object> getArguments()
    {
        return _arguments;
    }

    public boolean isAutoDelete()
    {
        return _autoDelete;
    }

    public AMQShortString getOwner()
    {
        return _owner;
    }

    public AuthorizationHolder getAuthorizationHolder()
    {
        return _authorizationHolder;
    }

    public void setAuthorizationHolder(final AuthorizationHolder authorizationHolder)
    {
        _authorizationHolder = authorizationHolder;
    }


    public VirtualHost getVirtualHost()
    {
        return _virtualHost;
    }

    public String getName()
    {
        return getNameShortString().toString();
    }

    public int compareTo(final AMQQueue o)
    {
        return _name.compareTo(o.getNameShortString());
    }

    public long getCreateTime()
    {
        return _createTime;
    }

    private QueueRunner _queueRunner = new QueueRunner(this);

    public void deliverAsync()
    {
        _stateChangeCount.incrementAndGet();

        _queueRunner.execute(_asyncDelivery);

    }

    public void deliverAsync(Subscription sub)
    {
        if(_exclusiveSubscriber == null)
        {
            deliverAsync();
        }
        else
        {
            SubFlushRunner flusher = (SubFlushRunner) sub.get(SUB_FLUSH_RUNNER);
            if(flusher == null)
            {
                flusher = new SubFlushRunner(sub);
                sub.set(SUB_FLUSH_RUNNER, flusher);
            }
            flusher.execute(_asyncDelivery);
        }

    }

    public void flushSubscription(Subscription sub) throws AMQException
    {
        // Access control
        if (!getVirtualHost().getSecurityManager().authoriseConsume(this))
        {
            throw new AMQSecurityException("Permission denied: " + getName());
        }
        flushSubscription(sub, Long.MAX_VALUE);
    }

    public boolean flushSubscription(Subscription sub, long iterations) throws AMQException
    {
        boolean atTail = false;
        final boolean keepSendLockHeld = iterations <=  SimpleAMQQueue.MAX_ASYNC_DELIVERIES;
        boolean queueEmpty = false;

        try
        {
            if(keepSendLockHeld)
            {
                sub.getSendLock();
            }
            while (!sub.isSuspended() && !atTail && iterations != 0)
            {
                try
                {
                    if(!keepSendLockHeld)
                    {
                        sub.getSendLock();
                    }

                    atTail = attemptDelivery(sub, true);
                    if (atTail && getNextAvailableEntry(sub) == null)
                    {
                        queueEmpty = true;
                    }
                    else if (!atTail)
                    {
                        iterations--;
                    }
                }
                finally
                {
                    if(!keepSendLockHeld)
                    {
                        sub.releaseSendLock();
                    }
                }
            }
        }
        finally
        {
            if(keepSendLockHeld)
            {
                sub.releaseSendLock();
            }
            if(queueEmpty)
            {
                sub.queueEmpty();
            }

            sub.flushBatched();

        }


        // if there's (potentially) more than one subscription the others will potentially not have been advanced to the
        // next entry they are interested in yet.  This would lead to holding on to references to expired messages, etc
        // which would give us memory "leak".

        if (!hasExclusiveSubscriber())
        {
            advanceAllSubscriptions();
        }
        return atTail;
    }

    /**
     * Attempt delivery for the given subscription.
     *
     * Looks up the next node for the subscription and attempts to deliver it.
     *
     *
     * @param sub
     * @param batch
     * @return true if we have completed all possible deliveries for this sub.
     * @throws AMQException
     */
    private boolean attemptDelivery(Subscription sub, boolean batch) throws AMQException
    {
        boolean atTail = false;

        boolean subActive = sub.isActive() && !sub.isSuspended();
        if (subActive)
        {

            QueueEntry node  = getNextAvailableEntry(sub);

            if (node != null && node.isAvailable())
            {
                if (sub.hasInterest(node) && mightAssign(sub, node))
                {
                    if (!sub.wouldSuspend(node))
                    {
                        if (sub.acquires() && !(assign(sub, node) && node.acquire(sub)))
                        {
                            // restore credit here that would have been taken away by wouldSuspend since we didn't manage
                            // to acquire the entry for this subscription
                            sub.restoreCredit(node);
                        }
                        else
                        {
                            deliverMessage(sub, node, batch);
                        }

                    }
                    else // Not enough Credit for message and wouldSuspend
                    {
                        //QPID-1187 - Treat the subscription as suspended for this message
                        // and wait for the message to be removed to continue delivery.
                        subActive = false;
                        node.addStateChangeListener(new QueueEntryListener(sub));
                    }
                }

            }
            atTail = (node == null) || (_entries.next(node) == null);
        }
        return atTail || !subActive;
    }

    protected void advanceAllSubscriptions() throws AMQException
    {
        SubscriptionList.SubscriptionNodeIterator subscriberIter = _subscriptionList.iterator();
        while (subscriberIter.advance())
        {
            SubscriptionList.SubscriptionNode subNode = subscriberIter.getNode();
            Subscription sub = subNode.getSubscription();
            if(sub.acquires())
            {
                getNextAvailableEntry(sub);
            }
            else
            {
                // TODO
            }
        }
    }

    private QueueEntry getNextAvailableEntry(final Subscription sub)
            throws AMQException
    {
        QueueContext context = (QueueContext) sub.getQueueContext();
        if(context != null)
        {
            QueueEntry lastSeen = context.getLastSeenEntry();
            QueueEntry releasedNode = context.getReleasedEntry();

            QueueEntry node = (releasedNode != null && lastSeen.compareTo(releasedNode)>=0) ? releasedNode : _entries.next(lastSeen);

            boolean expired = false;
            while (node != null && (!node.isAvailable() || (expired = node.expired()) || !sub.hasInterest(node) ||
                                    !mightAssign(sub,node)))
            {
                if (expired)
                {
                    expired = false;
                    if (node.acquire())
                    {
                        dequeueEntry(node);
                    }
                }

                if(QueueContext._lastSeenUpdater.compareAndSet(context, lastSeen, node))
                {
                    QueueContext._releasedUpdater.compareAndSet(context, releasedNode, null);
                }

                lastSeen = context.getLastSeenEntry();
                releasedNode = context.getReleasedEntry();
                node = (releasedNode != null && lastSeen.compareTo(releasedNode)>0) ? releasedNode : _entries.next(lastSeen);
            }
            return node;
        }
        else
        {
            return null;
        }
    }

    public boolean isEntryAheadOfSubscription(QueueEntry entry, Subscription sub)
    {
        QueueContext context = (QueueContext) sub.getQueueContext();
        if(context != null)
        {
            QueueEntry releasedNode = context.getReleasedEntry();
            return releasedNode != null && releasedNode.compareTo(entry) < 0;
        }
        else
        {
            return false;
        }
    }

    public List<Long> getMessagesOnTheQueue(int num)
    {
        return getMessagesOnTheQueue(num, 0);
    }

    public List<Long> getMessagesOnTheQueue(int num, int offset)
    {
        ArrayList<Long> ids = new ArrayList<Long>(num);
        QueueEntryIterator it = _entries.iterator();
        for (int i = 0; i < offset; i++)
        {
            it.advance();
        }

        for (int i = 0; i < num && !it.atTail(); i++)
        {
            it.advance();
            ids.add(it.getNode().getMessage().getMessageNumber());
        }
        return ids;
    }

    public AMQSessionModel getExclusiveOwningSession()
    {
        return _exclusiveOwner;
    }

    public void setExclusiveOwningSession(AMQSessionModel exclusiveOwner)
    {
        _exclusive = true;
        _exclusiveOwner = exclusiveOwner;
    }

    public ConfigStore getConfigStore()
    {
        return getVirtualHost().getConfigStore();
    }

    public long getMessageDequeueCount()
    {
        return  _dequeueCount.get();
    }

    public long getTotalEnqueueSize()
    {
        return _enqueueSize.get();
    }

    public long getTotalDequeueSize()
    {
        return _dequeueSize.get();
    }

    public long getByteTxnEnqueues()
    {
        return _byteTxnEnqueues.get();
    }

    public long getByteTxnDequeues()
    {
        return _byteTxnDequeues.get();
    }

    public long getMsgTxnEnqueues()
    {
        return _msgTxnEnqueues.get();
    }

    public long getMsgTxnDequeues()
    {
        return _msgTxnDequeues.get();
    }

    public long getPersistentByteEnqueues()
    {
        return _persistentMessageEnqueueSize.get();
    }

    public long getPersistentByteDequeues()
    {
        return _persistentMessageDequeueSize.get();
    }

    public long getPersistentMsgEnqueues()
    {
        return _persistentMessageEnqueueCount.get();
    }

    public long getPersistentMsgDequeues()
    {
        return _persistentMessageDequeueCount.get();
    }


    @Override
    public String toString()
    {
        return String.valueOf(getNameShortString());
    }

    @Override
    public void setDescription(String description)
    {
        if (description == null)
        {
            _arguments.remove(AMQQueueFactory.X_QPID_DESCRIPTION);
        }
        else
        {
            _arguments.put(AMQQueueFactory.X_QPID_DESCRIPTION, description);
        }
    }

    @Override
    public String getDescription()
    {
        return (String) _arguments.get(AMQQueueFactory.X_QPID_DESCRIPTION);
    }

}