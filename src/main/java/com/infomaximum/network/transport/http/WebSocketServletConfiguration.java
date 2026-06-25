package com.infomaximum.network.transport.http;

import com.infomaximum.network.session.UpgradeRequestImpl;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.jetty.ee10.websocket.server.*;

import java.util.Set;

/**
 * Настройка WebSocket-сервлета: выбирает подтверждаемый клиенту subprotocol только
 * среди реально поддерживаемых сервером, не доводя negotiation до сбоя Jetty.
 */
public class WebSocketServletConfiguration extends JettyWebSocketServlet {

    private final HttpTransport httpTransport;
    private final Set<String> supportedProtocols;

    /**
     * @param httpTransport      транспорт, обслуживающий соединение
     * @param supportedProtocols имена subprotocol'ов, зарегистрированных на сервере
     */
    public WebSocketServletConfiguration(@NonNull HttpTransport httpTransport, @NonNull Set<String> supportedProtocols) {
        this.httpTransport = httpTransport;
        this.supportedProtocols = supportedProtocols;
    }

    @Override
    public void configure(JettyWebSocketServletFactory factory) {
        factory.setMaxTextMessageSize(1 * 1024 * 1024);
        factory.setCreator(new JettyWebSocketCreatorImpl(httpTransport, supportedProtocols));
    }


    private static class JettyWebSocketCreatorImpl implements JettyWebSocketCreator {

        private final HttpTransport httpTransport;
        private final Set<String> supportedProtocols;

        public JettyWebSocketCreatorImpl(@NonNull HttpTransport httpTransport, @NonNull Set<String> supportedProtocols) {
            this.httpTransport = httpTransport;
            this.supportedProtocols = supportedProtocols;
        }

        @Override
        public @NonNull Object createWebSocket(@NonNull JettyServerUpgradeRequest request, @NonNull JettyServerUpgradeResponse response) {
            // Подтверждаем только тот subprotocol, который сервер действительно поддерживает
            // И который предложил клиент (request.getSubProtocols() — то, против чего Jetty
            // валидирует ответ). Иначе не подтверждаем ничего: handshake завершается без
            // subprotocol, а не падает с "selected a protocol not present in offered protocols".
            String selected = selectSupportedProtocol(request);
            if (selected != null) {
                response.setAcceptedSubProtocol(selected);
            }
            return new PacketWebSocketHandler(httpTransport, UpgradeRequestImpl.create(request));
        }

        private @Nullable String selectSupportedProtocol(JettyServerUpgradeRequest request) {
            for (String offered : request.getSubProtocols()) {
                if (supportedProtocols.contains(offered)) {
                    return offered;
                }
            }
            return null;
        }
    }
}
