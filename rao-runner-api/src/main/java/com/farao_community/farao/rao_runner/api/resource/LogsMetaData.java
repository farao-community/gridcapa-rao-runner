package com.farao_community.farao.rao_runner.api.resource;

public class LogsMetaData {

    private String processId;
    private String messageId;
    private String callerApp;

    public LogsMetaData(String processId, String messageId, String callerApp) {
        this.processId = processId;
        this.messageId = messageId;
        this.callerApp = callerApp;
    }

    public String getProcessId() {
        return processId;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getCallerApp() {
        return callerApp;
    }
}
