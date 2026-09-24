package org.example.study.application;

import org.example.study.domain.AsyncDetectAcceptedResult;
import org.example.study.domain.ContentType;
import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectRequest;
import org.example.study.domain.DetectResult;
import org.example.study.domain.DetectStatus;
import org.example.study.exception.DetectRequestException;
import org.example.study.rpc.AudioDetectRpcClient;
import org.example.study.rpc.ImageDetectRpcClient;
import org.example.study.rpc.TextDetectRpcClient;
import org.example.study.rpc.VideoDetectRpcClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证接入服务只进行 RPC 路由和故障开放。 */
class DetectServiceTests {

    private final TextDetectRpcClient textClient = mock(TextDetectRpcClient.class);
    private final ImageDetectRpcClient imageClient = mock(ImageDetectRpcClient.class);
    private final AudioDetectRpcClient audioClient = mock(AudioDetectRpcClient.class);
    private final VideoDetectRpcClient videoClient = mock(VideoDetectRpcClient.class);
    private DetectService service;

    @BeforeEach
    void setUp() {
        service = new DetectService(new DetectRequestValidator(), new TaskIdGenerator(), textClient,
                imageClient, audioClient, videoClient, event -> { });
    }

    @Test
    void routesSynchronousTextToTextRpc() {
        DetectResult rpcResult = new DetectResult("task", DetectStatus.SUCCEEDED, DetectAction.PASS,
                List.of(), false, "", "");
        when(textClient.detect(argThat(command -> command.contentType() == ContentType.TEXT))).thenReturn(rpcResult);

        var result = service.syncDetect("demo-app",
                new DetectRequest("client-1", "comment", "TEXT", "normal", null, null, null));

        assertThat(result).isSameAs(rpcResult);
    }

    @Test
    void rejectsSynchronousAudioBeforeRpc() {
        DetectRequest request = new DetectRequest("client-1", "voice", "AUDIO", null, null, "audio", null);

        assertThatThrownBy(() -> service.syncDetect("demo-app", request))
                .isInstanceOf(DetectRequestException.class)
                .hasMessage("Content type AUDIO is not supported in sync detection");
    }

    @Test
    void routesEveryAsynchronousModalityToItsRpc() {
        when(textClient.submit(argThat(command -> command.contentType() == ContentType.TEXT)))
                .thenAnswer(invocation -> accepted(invocation.getArgument(0, org.example.study.domain.DetectCommand.class)));
        when(imageClient.submit(argThat(command -> command.contentType() == ContentType.IMAGE)))
                .thenAnswer(invocation -> accepted(invocation.getArgument(0, org.example.study.domain.DetectCommand.class)));
        when(audioClient.submit(argThat(command -> command.contentType() == ContentType.AUDIO)))
                .thenAnswer(invocation -> accepted(invocation.getArgument(0, org.example.study.domain.DetectCommand.class)));
        when(videoClient.submit(argThat(command -> command.contentType() == ContentType.VIDEO)))
                .thenAnswer(invocation -> accepted(invocation.getArgument(0, org.example.study.domain.DetectCommand.class)));

        service.asyncDetect("app", new DetectRequest("1", "scene", "TEXT", "text", null, null, null));
        service.asyncDetect("app", new DetectRequest("2", "scene", "IMAGE", null, "image", null, null));
        service.asyncDetect("app", new DetectRequest("3", "scene", "AUDIO", null, null, "audio", null));
        service.asyncDetect("app", new DetectRequest("4", "scene", "VIDEO", null, null, null, "video"));

        verify(textClient).submit(argThat(command -> command.contentType() == ContentType.TEXT));
        verify(imageClient).submit(argThat(command -> command.contentType() == ContentType.IMAGE));
        verify(audioClient).submit(argThat(command -> command.contentType() == ContentType.AUDIO));
        verify(videoClient).submit(argThat(command -> command.contentType() == ContentType.VIDEO));
    }

    @Test
    void rpcFailureReturnsDegradedPassWithoutPromisingAsyncResult() {
        when(textClient.detect(org.mockito.ArgumentMatchers.any())).thenThrow(new IllegalStateException("offline"));
        when(audioClient.submit(org.mockito.ArgumentMatchers.any())).thenThrow(new IllegalStateException("offline"));

        var sync = service.syncDetect("app",
                new DetectRequest("1", "scene", "TEXT", "text", null, null, null));
        var async = service.asyncDetect("app",
                new DetectRequest("2", "scene", "AUDIO", null, null, "audio", null));

        assertThat(sync.degraded()).isTrue();
        assertThat(sync.action()).isEqualTo(DetectAction.PASS);
        assertThat(async.degraded()).isTrue();
        assertThat(async.resultExpected()).isFalse();
        assertThat(async.status()).isEqualTo(DetectStatus.FAILED);
    }

    private AsyncDetectAcceptedResult accepted(org.example.study.domain.DetectCommand command) {
        return new AsyncDetectAcceptedResult(command.taskId(), command.clientRequestId(),
                DetectStatus.ACCEPTED, command.submittedAt());
    }
}
