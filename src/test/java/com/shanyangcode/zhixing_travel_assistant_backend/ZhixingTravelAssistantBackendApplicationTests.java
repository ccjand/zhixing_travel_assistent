package com.shanyangcode.zhixing_travel_assistant_backend;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.bsc.langgraph4j.GraphDefinition.END;
import static org.bsc.langgraph4j.GraphDefinition.START;

@SpringBootTest
class ZhixingTravelAssistantBackendApplicationTests {



    public static class State extends MessagesState {
        public State(Map<String, Object> initData) {
            super(initData);
        }
    }


    public static AsyncNodeAction<State> makeNode(String msg) {
        return AsyncNodeAction.node_async(state -> Map.of("messages", msg));
    }

    public static void main(String[] args) throws GraphStateException {
        // 1) 子图（Child Graph）
        StateGraph<State> child = new StateGraph<>(State.SCHEMA, State::new)
                .addNode("child_step_1", makeNode("child:step1"))
                .addNode("child_step_2", makeNode("child:step2"))
                .addNode("child_step_3", makeNode("child:step3"))
                .addEdge(START, "child_step_1")
                .addEdge("child_step_1", "child_step_2")
                .addConditionalEdges(
                        "child_step_2",
                        s -> CompletableFuture.completedFuture("continue"),
                        Map.of(END, END, "continue", "child_step_3")
                )
                .addEdge("child_step_3", END);

        CompiledGraph<State> childCompiled = child.compile();


        // 2) 父图（Parent Graph）把子图当成一个节点挂进去
        StateGraph<State> parent = new StateGraph<>(State.SCHEMA, State::new)
                .addNode("step_1", makeNode("step1"))
                .addNode("step_2", makeNode("step2"))
                .addNode("step_3", makeNode("step3"))
                .addNode("subgraph", childCompiled)
                .addEdge(START, "step_1")
                .addEdge("step_1", "step_2")
                .addEdge("step_2", "subgraph")
                .addEdge("subgraph", "step_3")
                .addEdge("step_3", END);

        CompiledGraph<State> parentCompiled = parent.compile();

        // 3) 运行并输出每一步（包含子图内部步骤）
        for (var step : parentCompiled.stream(Map.of())) {
            System.out.println(step);
        }
    }

}
