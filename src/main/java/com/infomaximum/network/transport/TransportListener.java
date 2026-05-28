package com.infomaximum.network.transport;

import com.infomaximum.network.struct.UpgradeRequest;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Слушатель событий транспортного уровня сетевой подсистемы.
 *
 * <p>Реализация получает уведомления о подключении нового канала, входящих сообщениях
 * и отключении канала. Используется внутри модуля {@code network} для связывания
 * {@link Transport} с {@link com.infomaximum.network.NetworkImpl}.
 */
public interface TransportListener {

    /**
     * Уведомление о новом подключении.
     *
     * @param transport       транспорт, через который установлено подключение
     * @param channel         идентификатор канала (зависит от транспорта)
     * @param upgradeRequest  данные handshake-запроса
     */
    void onConnect(@NonNull Transport transport, @NonNull Object channel, @NonNull UpgradeRequest upgradeRequest);

    /**
     * Уведомление о входящем сообщении.
     *
     * @param transport  транспорт-источник
     * @param channel    идентификатор канала
     * @param message    тело сообщения
     */
    void incomingMessage(@NonNull Transport transport, @NonNull Object channel, @NonNull String message);

    /**
     * Уведомление о закрытии канала.
     *
     * @param transport   транспорт, в котором закрылся канал
     * @param channel     идентификатор канала
     * @param statusCode  код закрытия (значения, специфичные транспорту; для websocket — RFC 6455)
     * @param throwable   причина закрытия; {@code null} при штатном закрытии без ошибки
     */
    void onDisconnect(@NonNull Transport transport, @NonNull Object channel, int statusCode, @Nullable Throwable throwable);
}
