package com.infomaximum.network.transport.coretest.websocket;

import com.infomaximum.network.Network;
import com.infomaximum.network.protocol.standard.StandardProtocol;
import com.infomaximum.network.protocol.standard.packet.Packet;
import com.infomaximum.network.protocol.standard.packet.RequestPacket;
import com.infomaximum.network.protocol.standard.packet.ResponsePacket;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.jupiter.api.Assertions;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Тест, проверяющий, что на запрос приходит ответ и что нормальное закрытие
 * websocket-сессии не порождает ошибки на сервере (клиент получает close-event
 * с кодом {@link StatusCode#NORMAL}).
 */
public class CoreWSRequestTest {

    public static void test(Network network, int port) throws Exception {

        //Калбек ответа
        CompletableFuture<ResponsePacket> responseFuture = new CompletableFuture<ResponsePacket>();
        //Калбек close-event'а — фиксируем statusCode, чтобы проверить штатное закрытие
        CompletableFuture<Integer> closeStatusFuture = new CompletableFuture<Integer>();

        WebSocketClient client = new WebSocketClient();
        client.start();

        ClientEndPoint clientEndPoint = new ClientEndPoint(responseFuture, closeStatusFuture);
        URI serverURI = URI.create("ws://localhost:"  + port + "/ws");

        ClientUpgradeRequest upgradeRequest = new ClientUpgradeRequest();
        upgradeRequest.setSubProtocols(StandardProtocol.NAME);

        Future<Session> fut = client.connect(clientEndPoint, serverURI, upgradeRequest);

        //Ожидаем подключения
        Session session = fut.get();

        //Отправляем совоеобразный пакет пинга
        RequestPacket requestPacket = new RequestPacket(1, "support", "ping", null);
        session.sendText(requestPacket.serialize(), Callback.NOOP);

        ResponsePacket responsePacket = responseFuture.get(1, TimeUnit.MINUTES);

        Assertions.assertEquals(requestPacket.getId(), responsePacket.getId());

        session.close();//Закрываем соединение

        //Ждём close-event и проверяем, что сервер закрыл соединение штатно (1000),
        //а не с кодом 1011 (server unexpected error) — регрессия IS-770.
        Integer closeStatus = closeStatusFuture.get(30, TimeUnit.SECONDS);
        Assertions.assertEquals(StatusCode.NORMAL, closeStatus.intValue(),
                "Ожидался штатный close-code, фактический: " + closeStatus);
    }

    public static class ClientEndPoint implements Session.Listener.AutoDemanding {

        private final CompletableFuture<ResponsePacket> responseFuture;
        private final CompletableFuture<Integer> closeStatusFuture;

        public ClientEndPoint(CompletableFuture<ResponsePacket> responseFuture,
                              CompletableFuture<Integer> closeStatusFuture) {
            this.responseFuture = responseFuture;
            this.closeStatusFuture = closeStatusFuture;
        }

        @Override
        public void onWebSocketText(String message) {
            try {
                ResponsePacket responsePacket = (ResponsePacket) (Packet.parse((JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(message)));
                responseFuture.complete(responsePacket);
            } catch (Exception e) {
                Assertions.fail();
            }
        }

        @Override
        public void onWebSocketClose(int statusCode, String reason) {
            if (! responseFuture.isDone()) {
                responseFuture.completeExceptionally(new Exception("Соединение закрыто"));
            }
            closeStatusFuture.complete(statusCode);
        }

        @Override
        public void onWebSocketError(Throwable cause) {
            if (! responseFuture.isDone()) {
                responseFuture.completeExceptionally(new Exception("Ошибка соединения", cause));
            }
            if (! closeStatusFuture.isDone()) {
                closeStatusFuture.completeExceptionally(cause);
            }
        }
    }
}
