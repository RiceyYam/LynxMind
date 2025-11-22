package org.ricey_yam.lynxmind.client.event;

import lombok.Getter;
import lombok.Setter;
import org.ricey_yam.lynxmind.client.task.IAbsoluteTask;
import org.ricey_yam.lynxmind.client.task.ICoexistingTask;
import org.ricey_yam.lynxmind.client.task.Task;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public class LynxMindEndTickEvent<T extends Task<U>,U> {
    protected List<T> taskList = new ArrayList<>();

    public void tick(){
        if(taskList == null || taskList.isEmpty()) {
            return;
        }
        absoluteEventTick();
        coexistingEventTick();
    }

    public void register(T task){
        if(task == null) return;

        if(taskList == null) {
            taskList = new ArrayList<>();
        }
        var containTask = false;
        for (int i = taskList.size() - 1; i >= 0; i--) {
            var t = taskList.get(i);
            if(t == null) continue;
            if(t.getTaskType() == task.getTaskType()) {
                t.stop("原有的Task已被替换。");
                taskList.set(i, task);
                containTask = true;
            }
        }
        if(!containTask) taskList.add(task);
        task.start();
    }

    public void unregister(U taskType, String reason){
        if(taskList == null) {
            taskList = new ArrayList<>();
            return;
        }
        for (int i = taskList.size() - 1; i >= 0; i--) {
            var task = taskList.get(i);
            if (task != null && task.getTaskType() == taskType) {
                task.stop(reason);
                taskList.remove(i);
            }
        }
    }

    public void clean(String reason){
        for(var task : taskList){
            if(task != null) task.stop("Task被手动清理");
        }
        taskList.clear();
    }

    public T getTask(U taskType){
        for(var task : taskList){
            if(task == null) continue;
            if(task.getTaskType() == taskType) return task;
        }
        return null;
    }

    private void absoluteEventTick(){
        var highestWeightTasks = findHighestWeightTasks();

        if(highestWeightTasks != null && !highestWeightTasks.isEmpty()) {
            for (int i = 0; i < taskList.size(); i++) {
                var task = taskList.get(i);
                if(task == null) continue;
                if(!(task instanceof IAbsoluteTask)) continue;
                if(highestWeightTasks.contains(task)){
                    if (task.getCurrentTaskState() == Task.TaskState.IDLE) {
                        task.tick();
                    }
                    else if(task.getTaskType() == Task.TaskState.PAUSED){
                        task.start();
                    }
                }
                else{
                    if(task.getTaskType() == Task.TaskState.IDLE) {
                        task.pause();
                    }
                }
            }
        }
    }
    private void coexistingEventTick(){
        for (int i = 0; i < taskList.size(); i++) {
            var task = taskList.get(i);
            if(task == null) continue;
            if(!(task instanceof ICoexistingTask)) continue;
            if (task.getCurrentTaskState() == Task.TaskState.IDLE) {
                task.tick();
            }
        }
    }
    private List<T> findHighestWeightTasks() {
        Map<Double, List<T>> tasksByWeight = taskList.stream()
                .collect(Collectors.groupingBy(
                        task -> task instanceof IAbsoluteTask ? ((IAbsoluteTask) task).getWeight() : 0.0
                ));
        Optional<Double> maxWeightOptional = tasksByWeight.keySet().stream()
                .max(Comparator.naturalOrder());
        return maxWeightOptional
                .map(tasksByWeight::get)
                .orElse(List.of());
    }
}
