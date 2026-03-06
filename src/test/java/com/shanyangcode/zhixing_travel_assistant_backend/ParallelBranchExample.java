package com.shanyangcode.zhixing_travel_assistant_backend;

import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

public class ParallelBranchExample {

    public static final class PState extends AgentState {
        public static final Map<String, Channel<?>> SCHEMA = Map.of(
                "a", Channels.base(() -> ""),
                "a1", Channels.base(() -> ""),
                "a2", Channels.base(() -> ""),
                "a3", Channels.base(() -> ""),
                "b", Channels.base(() -> ""),
                "c", Channels.base(() -> "")
        );

        public PState(Map<String, Object> data) {
            super(data);
        }

        public String a() { return this.<String>value("a").orElse(""); }
        public String a1() { return this.<String>value("a1").orElse(""); }
        public String a2() { return this.<String>value("a2").orElse(""); }
        public String a3() { return this.<String>value("a3").orElse(""); }
        public String b() { return this.<String>value("b").orElse(""); }
        public String c() { return this.<String>value("c").orElse(""); }
    }

    public static void main(String[] args) throws GraphStateException {
        StateGraph<PState> g = new StateGraph<>(PState.SCHEMA, PState::new);

        g.addNode("A", AsyncNodeAction.node_async(s -> Map.of("a", "主节点A完成")));
        g.addNode("A1", AsyncNodeAction.node_async(s -> Map.of("a1", "分支A1完成")));
        g.addNode("A2", AsyncNodeAction.node_async(s -> Map.of("a2", "分支A2完成")));
        g.addNode("A3", AsyncNodeAction.node_async(s -> Map.of("a3", "分支A3完成")));

        g.addNode("B", AsyncNodeAction.node_async(s -> {
            Map<String, Object> updates = new HashMap<>();
            updates.put("b", "汇总B收到: [" + s.a1() + " | " + s.a2() + " | " + s.a3() + "]");
            return updates;
        }));

        g.addNode("C", AsyncNodeAction.node_async(s -> Map.of("c", "节点C完成")));

        g.addEdge(START, "A");
        g.addEdge("A", "A1");
        g.addEdge("A", "A2");
        g.addEdge("A", "A3");
        g.addEdge("A1", "B");
        g.addEdge("A2", "B");
        g.addEdge("A3", "B");
        g.addEdge("B", "C");
        g.addEdge("C", END);

        CompiledGraph<PState> graph = g.compile(
                CompileConfig.builder().checkpointSaver(new MemorySaver()).build()
        );

        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            RunnableConfig rc = RunnableConfig.builder()
                    .threadId("demo-parallel-branch")
                    .addParallelNodeExecutor("A", pool)
                    .build();

            PState out = graph.invokeFinal(GraphInput.args(Map.of()), rc).orElseThrow().state();

            System.out.println("并行分支结果");
            System.out.println("A  = " + out.a());
            System.out.println("A1 = " + out.a1());
            System.out.println("A2 = " + out.a2());
            System.out.println("A3 = " + out.a3());
            System.out.println("B  = " + out.b());
            System.out.println("C  = " + out.c());
        } finally {
            pool.shutdown();
        }
    }
}