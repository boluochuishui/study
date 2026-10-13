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
        var limits = new DetectionProperties();
        var validator = new DetectRequestValidator(limits,
                mock(org.example.study.baseSdk.storage.DetectionFileStore.class), new MediaSampleValidator(limits));
        service = new DetectService(validator, new TaskIdGenerator(), textClient,
                imageClient, audioClient, videoClient, event -> { });
    }

    @Test
    void routesSynchronousTextToTextRpc() {
        DetectResult rpcResult = new DetectResult("task", ContentType.TEXT, "COMPLETED",
                List.of(new org.example.study.domain.DetectItemResult(
                        "one", DetectStatus.SUCCEEDED, DetectAction.PASS, List.of(), false, "", "")));
        when(textClient.detect(argThat(command -> command.contentType() == ContentType.TEXT))).thenReturn(rpcResult);

        var result = service.syncDetect("demo-app",
                new DetectRequest("client-1", "comment", "TEXT", null, null, java.util.List.of(new org.example.study.domain.DetectItemRequest("one", "normal", null, null))));

        assertThat(result).isSameAs(rpcResult);
    }

    @Test
    void rejectsSynchronousAudioBeforeRpc() {
        DetectRequest request = new DetectRequest("client-1", "voice", "AUDIO", null, null, java.util.List.of(new org.example.study.domain.DetectItemRequest("one", null, "https://x/audio", null)));

        assertThatThrownBy(() -> service.syncDetect("demo-app", request))
                .isInstanceOf(DetectRequestException.class)
                .hasMessage("Synchronous media detection is not supported");
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

        service.asyncDetect("app", new DetectRequest("1", "scene", "TEXT", null, null,
                java.util.List.of(new org.example.study.domain.DetectItemRequest("one", "text", null, null))));
        service.asyncDetect("app", new DetectRequest("2", "scene", "IMAGE", null, null, java.util.List.of(new org.example.study.domain.DetectItemRequest("one", null, "https://x/image", null))));
        service.asyncDetect("app", new DetectRequest("3", "scene", "AUDIO", null, null, java.util.List.of(new org.example.study.domain.DetectItemRequest("one", null, "https://x/audio", null))));
        service.asyncDetect("app", new DetectRequest("4", "scene", "VIDEO", null, null, java.util.List.of(new org.example.study.domain.DetectItemRequest("one", null, "https://x/video", null))));

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
                new DetectRequest("1", "scene", "TEXT", null, null, java.util.List.of(new org.example.study.domain.DetectItemRequest("one", "text", null, null))));
        var async = service.asyncDetect("app",
                new DetectRequest("2", "scene", "AUDIO", null, null, java.util.List.of(new org.example.study.domain.DetectItemRequest("one", null, "https://x/audio", null))));

        assertThat(sync.getResults().getFirst().isDegraded()).isTrue();
        assertThat(sync.getResults().getFirst().getAction()).isEqualTo(DetectAction.PASS);
        assertThat(async.isDegraded()).isTrue();
        assertThat(async.isResultExpected()).isFalse();
        assertThat(async.getStatus()).isEqualTo(DetectStatus.FAILED);
    }

    private AsyncDetectAcceptedResult accepted(org.example.study.domain.DetectCommand command) {
        return new AsyncDetectAcceptedResult(command.taskId(), command.clientRequestId(),
                DetectStatus.ACCEPTED, command.submittedAt());
    }
}
