#program step(t).

% ================================================================
% STEP — per-timestep planning rules
% ================================================================

% Mark this timestep
time(t).

% ----------------------------------------------------------------
% From propagation.lp — inertia for availability and dimensions
% ----------------------------------------------------------------

holds(t, avail(WF)) :-
    holds(t-1, avail(WF)).

holds(t, dim(WF, V, Cat)) :-
    holds(t-1, dim(WF, V, Cat)),
    holds(t, avail(WF)).

% ----------------------------------------------------------------
% From unit_reasoning.lp — unit carry-forward
% ----------------------------------------------------------------

holds(t, unit(WF, U)) :-
    holds(t-1, unit(WF, U)),
    holds(t, avail(WF)).

% ----------------------------------------------------------------
% From artifact_state.lp — inertia and transition firing
% ----------------------------------------------------------------

% Positive persistence
holds(t, state(Artifact, Value)) :-
    holds(t-1, state(Artifact, Value)),
    not -holds(t, state(Artifact, Value)).

% Negative persistence
-holds(t, state(Artifact, Value)) :-
    -holds(t-1, state(Artifact, Value)),
    not holds(t, state(Artifact, Value)).

% Transition firing — initiation
holds(t, state(Artifact, NewState)) :-
    initiates(Tool, Artifact, NewState),
    occurs(t, run(Tool)).

% Transition firing — termination
-holds(t, state(Artifact, OldState)) :-
    terminates(Tool, Artifact, OldState),
    occurs(t, run(Tool)).

% Consistency: artifact cannot hold two states simultaneously
:- holds(t, state(Artifact, S1)),
   holds(t, state(Artifact, S2)),
   S1 != S2.

% ----------------------------------------------------------------
% From tool_choice.lp — run choice, variant selection, port binding
% ----------------------------------------------------------------

% Run choice
{ occurs(t, run(Tool)) } :- tool(Tool), not tool_has_input(Tool).
{ occurs(t, run(Tool)) } :- tool(Tool), tool_has_input(Tool).

% Variant selection: exactly one input group per run
1 { occurs(t, use_variant(Tool, TI_ID)) : tool_input_variant(Tool, TI_ID) } 1 :-
    occurs(t, run(Tool)),
    tool_has_input(Tool).

occurs(t, run(Tool)) :- occurs(t, use_variant(Tool, _)).

% Port satisfaction per category
port_cat_ok(t, Port, WF, Cat) :-
    holds(t, avail(WF)),
    port_requires(Port, ReqV, Cat),
    holds(t, dim(WF, V, Cat)),
    compatible(V, ReqV).

% Eligible item: all categories satisfied
eligible(t, Port, WF) :-
    holds(t, avail(WF)),
    input_port(_, Port),
    port_cat_ok(t, Port, WF, Cat) : port_requires(Port, _, Cat).

% Port binding: exactly one eligible item per port
1 { occurs(t, bind(Tool, Port, WF)) : eligible(t-1, Port, WF) } 1 :-
    occurs(t, use_variant(Tool, TI_ID)),
    input_port(TI_ID, Port).

% ----------------------------------------------------------------
% From output_production.lp — output port selection and dimensions
% ----------------------------------------------------------------

% Output selection: exactly one port per tool_output group
1 { occurs(t, output(Tool, TOut_ID, Port)) : output_port(TOut_ID, Port) } 1 :-
    occurs(t, run(Tool)),
    tool_has_output(Tool),
    tool_output(Tool, TOut_ID).

% Output availability
holds(t, avail(out(t, Tool, Port))) :-
    occurs(t, output(Tool, _, Port)).

% Output dimensions — Case A: terminal value
holds(t, dim(out(t, Tool, Port), V, Cat)) :-
    occurs(t, output(Tool, _, Port)),
    dimension(Port, (V, Cat)),
    is_terminal(V).

% Output dimensions — Case B: non-terminal, planner picks terminal descendant
1 { occurs(t, output_dim(Tool, Port, Descendant, Cat)) :
    compatible(Descendant, V),
    is_terminal(Descendant) } 1 :-
    occurs(t, output(Tool, _, Port)),
    dimension(Port, (V, Cat)),
    not is_terminal(V).

holds(t, dim(out(t, Tool, Port), Descendant, Cat)) :-
    occurs(t, output_dim(Tool, Port, Descendant, Cat)).

% ----------------------------------------------------------------
% From unit_reasoning.lp — unit production from tool outputs
% ----------------------------------------------------------------

% Case 1: explicit port_unit declaration
holds(t, unit(out(t, Tool, Port), U)) :-
    occurs(t, output(Tool, _, Port)),
    port_unit(Port, U).

% Case 2: strict mode — no declaration → unit is none
holds(t, unit(out(t, Tool, Port), none)) :-
    unit_mode(strict),
    occurs(t, output(Tool, _, Port)),
    not port_unit(Port, _).

% Case 3: agnostic mode — inherit from homogeneous inputs
input_root(t, Tool, Root) :-
    occurs(t, bind(Tool, _, WF)),
    holds(t, unit(WF, U)),
    U != none,
    unit_root(U, Root).

single_input_root(t, Tool) :-
    occurs(t, run(Tool)),
    #count { Root : input_root(t, Tool, Root) } = 1.

holds(t, unit(out(t, Tool, Port), U)) :-
    unit_mode(agnostic),
    occurs(t, output(Tool, _, Port)),
    not port_unit(Port, _),
    single_input_root(t, Tool),
    occurs(t, bind(Tool, _, WF)),
    holds(t, unit(WF, U)),
    U != none.

% Unit binding compatibility constraints
:- occurs(t, bind(Tool, Port, WF)),
   port_unit(Port, UPort),
   holds(t, unit(WF, UData)),
   UData != none,
   not units_compatible(UPort, UData).

:- unit_mode(strict),
   occurs(t, bind(Tool, Port, WF)),
   port_unit(Port, _),
   not holds(t, unit(WF, _)).

% ----------------------------------------------------------------
% From reachability.lp
% ----------------------------------------------------------------

consumes_output(t, Tool) :-
    occurs(t, bind(Tool, _, out(_, _, _))).

% Root: no-input tools
is_root(t, Tool) :-
    occurs(t, run(Tool)),
    not tool_has_input(Tool).

% Root: has inputs but none are tool outputs
is_root(t, Tool) :-
    occurs(t, run(Tool)),
    tool_has_input(Tool),
    not consumes_output(t, Tool).

reachable(t, Tool) :- is_root(t, Tool).

reachable(t, Tool2) :-
    reachable(T1, Tool1),
    occurs(T1, output(Tool1, _, Port)),
    occurs(t, bind(Tool2, _, out(T1, Tool1, Port))),
    t > T1.

:- occurs(t, run(Tool)), not reachable(t, Tool).

% ----------------------------------------------------------------
% From goal.lp — derivation only (hard constraint is in check.lp)
% Atoms are timestamped with t to avoid redefinition across groundings.
% ----------------------------------------------------------------

goal_dim_match(t, GoalID, WF, GoalV, Cat) :-
    goal_output(GoalID, GoalV, Cat),
    holds(t, avail(WF)),
    holds(t, dim(WF, ActualV, Cat)),
    compatible(ActualV, GoalV).

goal_dim_missing(t, GoalID, WF) :-
    goal_output(GoalID, GoalV, Cat),
    holds(t, avail(WF)),
    not goal_dim_match(t, GoalID, WF, GoalV, Cat).

goal_satisfied_by(t, GoalID, WF) :-
    holds(t, avail(WF)),
    goal_output(GoalID, _, _),
    goal_dim_match(t, GoalID, WF, _, _),
    not goal_dim_missing(t, GoalID, WF).

goal_satisfied(t, GoalID) :-
    goal_output(GoalID, _, _),
    goal_satisfied_by(t, GoalID, WF).

holds(t, goal) :-
    goal_satisfied(t, GoalID) : goal_output(GoalID, _, _).

% Goal inertia: once reached, persists
holds(t, goal) :- holds(t-1, goal).

% ----------------------------------------------------------------
% From properties.lp
% pipeline_has(Prop) is omitted here — it is non-timestamped and would
% cause redefinition errors in incremental solving. Use pipeline_has_at(t, Prop)
% for per-step tracking; a global accumulator can be added in user constraints.
% ----------------------------------------------------------------

pipeline_has_at(t, Prop) :-
    tool_property(Tool, Prop),
    occurs(t, run(Tool)).

pipeline_has_at(t, Prop) :-
    port_property(Tool, Port, ReqV, Cat, Prop),
    occurs(t, bind(Tool, Port, WF)),
    holds(t, dim(WF, ActualV, Cat)),
    compatible(ActualV, ReqV).


% ----------------------------------------------------------------
% From requirements.lp — constraints only (static derivations in base.lp)
% ----------------------------------------------------------------

:- requirement_filtering_active, occurs(t, run(Tool)), not tool_req_compatible(Tool).
:- requirement_filtering_active, occurs(t, bind(Tool, Port, _)), not port_env_compatible(Tool, Port).

% ----------------------------------------------------------------
% From constraints.lp
% ----------------------------------------------------------------

% Contiguous execution: no gaps in the plan
:- t > 1, occurs(t, run(_)), not occurs(t-1, run(_)).

% No runs after goal is reached
:- occurs(t, run(_)), holds(t-1, goal).

% No repeated port binding for same tool instance across runs
:- occurs(T1, run(Tool)), occurs(t, run(Tool)), T1 < t,
   occurs(T1, bind(Tool, Port, WF)),
   occurs(t,  bind(Tool, Port, WF)).

% ----------------------------------------------------------------
% From tool_repetition_constraints.lp
% ----------------------------------------------------------------

:- occurs(t, run(Tool)), not multi_run(Tool), occurs(T1, run(Tool)), T1 < t.

% ----------------------------------------------------------------
% Sequential workflow: at most one tool runs per timestep
% (mirrors the SAT automaton which allocates exactly one tool slot per step)
% ----------------------------------------------------------------

:- 2 { occurs(t, run(_)) }.

