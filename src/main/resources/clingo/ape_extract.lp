#program step(t).

% ================================================================
% APE EXTRACTION — per-timestep atoms read back by SolutionWorkflow
% ================================================================
% Predicate-based #show directives work correctly across incremental
% groundings: re-registration at each step(t) invocation is idempotent.

% Which tool runs at this step.
tool_at_time(t, Tool) :- occurs(t, run(Tool)).
#show tool_at_time/2.

% Input bindings at this step: (step, port-id, workflow-artifact).
ape_bind(t, Port, WF) :- occurs(t, bind(_, Port, WF)).
#show ape_bind/3.

% Type annotations for tool outputs created at THIS step only.
% The out(t,...) pattern ensures the atom is derived only when the output
% is first produced, preventing redefinition across incremental steps.
% Workflow-input annotations are emitted as base facts by ClingoSynthesisEngine.
ape_holds_dim(out(t, Tool, Port), V, Cat) :- holds(t, dim(out(t, Tool, Port), V, Cat)).
#show ape_holds_dim/3.

% Workflow outputs — t in the head prevents redefinition when the goal
% remains satisfied across multiple steps.
ape_goal_out(t, GoalID, WF) :- goal_satisfied_by(t, GoalID, WF).
#show ape_goal_out/3.
