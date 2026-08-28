package common.dto;

public class Response {
    private final boolean success;
    private final String error;
    private final Object data;

    private Response(boolean success, String error, Object data){
        this.success = success;
        this.error = error;
        this.data = data;
    }

    public static Response ok(Object data){
        return new Response(true, null, data);
    }

    public static Response ok(){
        return new Response(true, null, null);
    }

    public static Response error(String errorMessage){
        return new Response(false, errorMessage, null);
    }

    public boolean isSuccess(){ return success; }
    public String getError(){ return error; }
    public Object getData(){ return data; }
}
