package server.handler;

import com.google.gson.JsonObject;
import common.protocol.response.ServerResponse;

public interface CommandHandler {
    ServerResponse<?> handle(JsonObject request);
}
