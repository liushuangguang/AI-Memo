package com.newtech.note.client.entity.coze;

/**
 * coze workflow response
 * @param code
 * @param cost
 * @param data
 * @param debugUrl
 * @param msg
 * @param token
 */
public record CozeWorkflowRsp(int code, String cost, String data, String debugUrl, String msg, int token) {
}
