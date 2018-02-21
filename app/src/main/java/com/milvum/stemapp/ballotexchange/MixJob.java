package com.milvum.stemapp.ballotexchange;

/**
 * .
 */

public enum MixJob {
    REQUEST(RequestJobService.class, 0),
    PAYMENT(PaymentJobService.class, 1),
    WHISPER(WhisperJobService.class, 2),
    FINISH(FinishJobService.class, 3),
    MASK(MaskVoteJobService.class, 4);

    private Class jobClass;
    private int id;
    MixJob(Class jobClass, int id) {
        this.jobClass = jobClass;
        this.id = id;
    }

    Class getJobClass() {
        return this.jobClass;
    }

    int getId() {
        return this.id;
    }
}
