package com.shanyangcode.zhixing_travel_assistant_backend.agent.requirement;

import com.shanyangcode.zhixing_travel_assistant_backend.agent.accommodation.AccommodationPlannerAgent;
import com.shanyangcode.zhixing_travel_assistant_backend.agent.food.FoodPlannerAgent;
import com.shanyangcode.zhixing_travel_assistant_backend.agent.state.TravelWorkflowState;
import com.shanyangcode.zhixing_travel_assistant_backend.agent.transportation.TransportPlannerAgent;
import com.shanyangcode.zhixing_travel_assistant_backend.agent.transportation.subagents.DrivingTransportSubAgent;
import com.shanyangcode.zhixing_travel_assistant_backend.agent.transportation.subagents.FlightTransportSubAgent;
import com.shanyangcode.zhixing_travel_assistant_backend.agent.transportation.subagents.TrainTransportSubAgent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.*;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.checkpoint.PostgresSaver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
@Slf4j
@SuppressWarnings("ALL")
public class RequirementAgent {

    @Autowired
    private RequirementAgentService requirementAgentService;

    @Autowired
    @Qualifier("taskExecutor")
    private Executor taskExecutor;

    @Autowired
    private TransportPlannerAgent transportPlannerAgent;

    @Autowired
    private AccommodationPlannerAgent accommodationPlannerAgent;

    @Autowired
    private FoodPlannerAgent foodPlannerAgent;

    @Autowired
    private DrivingTransportSubAgent drivingTransportSubAgent;

    @Autowired
    private TrainTransportSubAgent trainTransportSubAgent;

    @Autowired
    private FlightTransportSubAgent flightTransportSubAgent;

    private CompiledGraph<TravelWorkflowState> graph;

    @Autowired
    private PostgresSaver checkpointSaver;

    @PostConstruct
    public void initGraph() throws GraphStateException {
        //父图
        StateGraph<TravelWorkflowState> workflow = new StateGraph<>(TravelWorkflowState.scheme, TravelWorkflowState::new);
        //交通子图
        StateGraph<TravelWorkflowState> transportSubgraph = new StateGraph<>(TravelWorkflowState.scheme, TravelWorkflowState::new);

        //收集缺失字段信息
        workflow.addNode("collect", AsyncNodeAction.node_async(state -> {
            log.info("STATE before node=collect {}", state);
            //判断是否确实字段，缺失字段就跳转到askMissing节点去收集, 否则就跳转到planParallel节点去执行
            boolean missing = state.missing();
            if (missing) {
                String userMessage = """
                        {
                            "sessionId": %s,
                            "userText": %s,
                            "missing": %b,
                            "firstMissing": "",
                            "requirements": {
                                "origin": %s,
                                "destination": %s,
                                "startDate": %s,
                                "people": %s,
                                "special": %s
                            }
                        }
                        """.formatted(state.sessionId(), state.userText(), true, state.requirements().getOrigin(),
                        state.requirements().getDestination(), state.requirements().getStartDate(),
                        state.requirements().getPeople(), state.requirements().getSpecial());

                RequirementInfo res = requirementAgentService.chat(state.sessionId(), userMessage);
//                log.info("RequirementAgent AI回复 {}", res);

                if (res.getMissing()) {
                    Map<String, Object> updates = Map.of(
                            "nextStep", "askMissing",
                            "missing", true,
                            "firstMissing", res.getFirstMissing(),
                            "requirements", res.getRequirements()
                    );
                    log.info("UPDATES node=collect {}", updates);
                    return updates;
                } else {
                    Map<String, Object> updates = Map.of(
                            "nextStep", "planParallel",
                            "missing", false,
                            "firstMissing", "",
                            "requirements", res.getRequirements()
                    );
                    log.info("UPDATES node=collect {}", updates);
                    return updates;
                }
            } else {
                Map<String, Object> updates = Map.of(
                        "nextStep", "planParallel",
                        "missing", false,
                        "firstMissing", "",
                        "requirements", state.requirements()
                );
                log.info("UPDATES node=collect {}", updates);
                return updates;
            }
        }));

        //询问缺失的字段信息
        workflow.addNode("askMissing", AsyncNodeAction.node_async(state -> {
            log.info("STATE before node=askMissing {}", state);
            log.info("正在执行askMissing");
            String output = MissingFieldPrompt.ask(state.firstMissing());
            Map<String, Object> updates = Map.of("output", output);
            log.info("UPDATES node=askMissing {}", updates);
            return updates;
        }));

        //中转
        workflow.addNode("planParallel", AsyncNodeAction.node_async(state -> {
            log.info("STATE before node=planParallel {}", state);
            log.info("正在执行planParallel");
            Map<String, Object> updates = Map.of();
            log.info("UPDATES node=planParallel {}", updates);
            return updates;
        }));

        workflow.addNode("accommodationPlan", AsyncNodeAction.node_async(state -> {
            log.info("STATE before node=accommodationPlan {}", state);
            log.info("正在执行accommodationPlan");
            String res = accommodationPlannerAgent.execute(state.sessionId(), state.requirements());
            log.info("accommodationPlan AI的回复:{}", res);
            Map<String, Object> updates = Map.of("nextStep", "assemble", "accommodationOutput", res);
            log.info("UPDATES node=accommodationPlan {}", updates);
            return updates;
        }));

        workflow.addNode("foodPlan", AsyncNodeAction.node_async(state -> {
            log.info("STATE before node=foodPlan {}", state);
            log.info("正在执行foodPlan");
            String res = foodPlannerAgent.execute(state.sessionId(), state.requirements().getDestination());
//            log.info("foodPlan AI的回复:{}", res);
            Map<String, Object> updates = Map.of("foodOutput", res, "nextStep", "assemble");
            log.info("UPDATES node=foodPlan {}", updates);
            return updates;
        }));

        //中转
        transportSubgraph.addNode("transportPlan", AsyncNodeAction.node_async(state -> {
            log.info("STATE before node=transportPlan {}", state);
            log.info("正在执行transportPlan");
            Map<String, Object> updates = Map.of();
            log.info("UPDATES node=transportPlan {}", updates);
            return updates;
        }));

        //自驾
        transportSubgraph.addNode("drivePlan", AsyncNodeAction.node_async(state -> {
            log.info("STATE before node=drivePlan {}", state);
            log.info("正在执行drivePlan");
            String res = drivingTransportSubAgent.execute(state.sessionId(), state.requirements());
//            log.info("自驾 AI的回复:{}", res);
            Map<String, Object> updates = Map.of("nextStep", "transportAssemble", "drivingTransportOutput", res);
            log.info("UPDATES node=drivePlan {}", updates);
            return updates;
        }));

        //高铁
        transportSubgraph.addNode("trainPlan", AsyncNodeAction.node_async(state -> {
            log.info("STATE before node=trainPlan {}", state);
            log.info("正在执行trainPlan");
            String res = trainTransportSubAgent.execute(state.sessionId(), state.requirements());
//            log.info("高铁 AI的回复:{}", res);
            Map<String, Object> updates = Map.of("nextStep", "transportAssemble", "trainTransportOutput", res);
            log.info("UPDATES node=trainPlan {}", updates);
            return updates;
        }));

        //飞机
        transportSubgraph.addNode("flightPlan", AsyncNodeAction.node_async(state -> {
            log.info("STATE before node=flightPlan {}", state);
            log.info("正在执行flightPlan");
            String res = flightTransportSubAgent.execute(state.sessionId(), state.requirements());
//            log.info("飞机 AI的回复:{}", res);
            Map<String, Object> updates = Map.of("nextStep", "transportAssemble", "flightTransportOutput", res);
            log.info("UPDATES node=flightPlan {}", updates);
            return updates;
        }));

        //交通汇总
        transportSubgraph.addNode("transportAssemble", AsyncNodeAction.node_async(state -> {
            log.info("STATE before node=transportAssemble {}", state);
            log.info("正在执行transportAssemble");
            String res = transportPlannerAgent.execute(state.sessionId(), state);
//            log.info("交通汇总 AI的回复:{}", res);
            Map<String, Object> updates = Map.of("nextStep", "assemble", "transportOutput", res);
            log.info("UPDATES node=transportAssemble {}", updates);
            return updates;
        }));

        workflow.addNode("assemble", AsyncNodeAction.node_async(state -> {
            log.info("STATE before node=assemble {}", state);
            //汇总
            log.info("正在执行assemble");
            String transport = state.transportOutput() == null ? "" : state.transportOutput().trim();
            String accommodation = state.accommodationOutput() == null ? "" : state.accommodationOutput().trim();
            String food = state.foodOutput() == null ? "" : state.foodOutput().trim();

            Requirements requirements = state.requirements();
            String origin = Optional.ofNullable(requirements.getOrigin()).orElse("未提供");
            String destination = Optional.ofNullable(requirements.getDestination()).orElse("未提供");
            String startDate = Optional.ofNullable(requirements.getStartDate()).orElse("未提供");
            String people = Optional.ofNullable(requirements.getPeople()).orElse("未提供");
            String special = Optional.ofNullable(requirements.getSpecial()).orElse("未提供");

            String output = """
                    【行程规划汇总】
                    出发地：%s
                    目的地：%s
                    出发时间：%s
                    人数：%s
                    偏好：%s
                    
                    
                    【交通方案】
                    %s
                    
                    
                    【住宿方案】
                    %s
                    
                    
                    【美食方案】
                    %s
                    
                    """.formatted(origin, destination, startDate, people, special, transport, accommodation, food);

            Map<String, Object> updates = Map.of("output", output);
            log.info("UPDATES node=assemble {}", updates);
            return updates;
        }));

        // 子图边
        transportSubgraph.addEdge(GraphDefinition.START, "transportPlan");
        transportSubgraph.addEdge("transportPlan", "drivePlan");
        transportSubgraph.addEdge("transportPlan", "trainPlan");
        transportSubgraph.addEdge("transportPlan", "flightPlan");

        transportSubgraph.addEdge("drivePlan", "transportAssemble");
        transportSubgraph.addEdge("trainPlan", "transportAssemble");
        transportSubgraph.addEdge("flightPlan", "transportAssemble");

        transportSubgraph.addEdge("transportAssemble", GraphDefinition.END);


        //父图
        workflow.addEdge(GraphDefinition.START, "collect");
        //当执行到collect节点时，根据nextStep的值，选择执行askMissing或planParallel节点
        workflow.addConditionalEdges("collect",
                state -> CompletableFuture.completedFuture(state.nextStep()),
                Map.of("askMissing", "askMissing", "planParallel", "planParallel"));

        //编译子图 & 挂载子图
        CompiledGraph<TravelWorkflowState> travelWorkflowStateCompiledGraph = transportSubgraph.compile(
                CompileConfig.builder().checkpointSaver(checkpointSaver).build()
        );

        workflow.addSubgraph("transportSubgraph", travelWorkflowStateCompiledGraph);

        workflow.addEdge("askMissing", "collect");

        workflow.addEdge("planParallel", "transportSubgraph");
        workflow.addEdge("planParallel", "accommodationPlan");
        workflow.addEdge("planParallel", "foodPlan");
        workflow.addEdge("transportSubgraph", "assemble");
        workflow.addEdge("accommodationPlan", "assemble");
        workflow.addEdge("foodPlan", "assemble");

        workflow.addEdge("assemble", GraphDefinition.END);


        CompileConfig compileConfig = CompileConfig.builder()
                .checkpointSaver(checkpointSaver)
                .interruptsAfter(java.util.List.of("askMissing"))
                .build();

        this.graph = workflow.compile(compileConfig);

        System.out.println(graph.getGraph(GraphRepresentation.Type.MERMAID));
    }


    public Flux<String> execute(String sessionId, String userText) {
        return executeState(sessionId, userText).map(TravelWorkflowState::output)
                .flatMapMany(output -> {
                    if (output == null || output.isBlank()) return Flux.just("没有结果哦~~");
                    return Flux.just(output);
                });
    }

    public Mono<TravelWorkflowState> executeState(String sessionId, String userText) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("sessionId", sessionId);
        inputs.put("userText", userText);

        RunnableConfig config = RunnableConfig.builder()
                .threadId(sessionId)
                .addParallelNodeExecutor("planParallel", taskExecutor)
                .addParallelNodeExecutor("transportPlan", taskExecutor)
                .build();

        return Mono.fromCallable(() -> {
                    GraphInput input;
                    String nextNode = graph.stateOf(config).map(s -> s.next()).orElse(null);
                    if (nextNode != null && !GraphDefinition.END.equals(nextNode)) {
                        input = GraphInput.resume(inputs);
                    } else {
                        input = GraphInput.args(inputs);
                    }
                    return graph.invokeFinal(input, config).orElse(null);
                })
                .onErrorResume(e -> {
                    log.error("Error executing graph", e);
                    return Mono.empty();
                })
                .flatMap(out -> {
                    if (out == null) return Mono.empty();
                    TravelWorkflowState state = out.state();
                    if (state != null) {
                        log.info("STATE after node=invokeFinal {}", state);
                    }
                    return Mono.just(state);
                });
    }
}
