package com.hometask.server.listener;

import com.hometask.server.dto.MessageResult;
import com.hometask.server.service.PendingReplyRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageReplyListenerTest {

    @Mock
    private PendingReplyRegistry pendingReplyRegistry;

    @Test
    void onReplyDelegatesToTheRegistry() {
        MessageReplyListener listener = new MessageReplyListener(pendingReplyRegistry);
        MessageResult result = new MessageResult();
        result.setCorrelationId("corr-1");
        result.setSuccess(true);

        listener.onReply(result);

        verify(pendingReplyRegistry).complete(result);
    }
}
