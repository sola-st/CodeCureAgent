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

class LoggerAndNotifier {
    private final LogActor logActor;
    private AMQQueue.NotificationListener notificationListener;

    public LogSubject getLogSubject()
    {
        return _logSubject;
    }

    public LogActor getLogActor()
    {
        return _logActor;
    }

    private void checkForNotification(ServerMessage<?> msg) throws AMQException
    {
        final Set<NotificationCheck> notificationChecks = getNotificationChecks();
        final AMQQueue.NotificationListener listener = _notificationListener;

        if(listener != null && !notificationChecks.isEmpty())
        {
            final long currentTime = System.currentTimeMillis();
            final long thresholdTime = currentTime - getMinimumAlertRepeatGap();

            for (NotificationCheck check : notificationChecks)
            {
                if (check.isMessageSpecific() || (_lastNotificationTimes[check.ordinal()] < thresholdTime))
                {
                    if (check.notifyIfNecessary(msg, this, listener))
                    {
                        _lastNotificationTimes[check.ordinal()] = currentTime;
                    }
                }
            }
        }
    }

    public void setNotificationListener(AMQQueue.NotificationListener listener)
    {
        _notificationListener = listener;
    }
}