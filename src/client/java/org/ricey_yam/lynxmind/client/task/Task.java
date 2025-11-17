package org.ricey_yam.lynxmind.client.task;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Task<T> {
    protected T taskType;
    protected int tickTimer;
    protected TaskState currentTaskState;
    public abstract void start();
    public abstract void tick() throws InterruptedException;
    public abstract void stop(String cancelReason);
    public enum TaskState{
        IDLE,
        FINISHED,
        FAILED
    }
}
