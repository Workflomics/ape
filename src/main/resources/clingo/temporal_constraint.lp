% ================================================================
% TEMPORAL CONSTRAINTS (multi-shot)
% #program check(t) for sel_run_at and enforcement rules
% #program base for sel_used (static, derived from before/after facts)
%
% Activation: before/after/eventually_before/eventually_after in config
% Docs: DOCS/multi_shot/constraints/temporal_constraint.md
%
% All selector resolution atoms are namespaced sel_run_at(t, T, Sel)
% to prevent redefinition errors across check groundings. The t prefix
% is the check horizon; T is the actual step the tool ran at.
%
% sel_used is in #program base because before/after/eventually_* facts
% are static and known at base grounding time.
% ================================================================

#program base.

% ----------------------------------------------------------------
% sel_used — collect selectors referenced in temporal constraints
% Prevents and(...) rules from causing infinite grounding.
% ----------------------------------------------------------------

sel_used(Sel) :- before(Sel, _).
sel_used(Sel) :- before(_, Sel).
sel_used(Sel) :- after(Sel, _).
sel_used(Sel) :- after(_, Sel).
sel_used(Sel) :- eventually_before(Sel, _).
sel_used(Sel) :- eventually_before(_, Sel).
sel_used(Sel) :- eventually_after(Sel, _).
sel_used(Sel) :- eventually_after(_, Sel).
sel_used(SelA) :- sel_used(and(SelA, _)).
sel_used(SelB) :- sel_used(and(_, SelB)).


#program check(t).

% ----------------------------------------------------------------
% SELECTOR RESOLUTION
% sel_run_at(t, T, Sel) — Sel is satisfied at step T within horizon t
% ----------------------------------------------------------------

% Concrete tool name
sel_run_at(t, T, Tool) :-
    query(t), tool(Tool), occurs(T, run(Tool)).

% Abstract tool class — matches any member that runs
sel_run_at(t, T, Class) :-
    query(t), tool_abstract_class(Class),
    occurs(T, run(Tool)), tool_is_a(Tool, Class).

% Tool with a specific input type
sel_run_at(t, T, with_input_type(Tool, TypeVal)) :-
    query(t),
    occurs(T, run(Tool)),
    occurs(T, bind(Tool, _, WF)),
    holds(T-1, dim(WF, TypeVal, type)).

% Tool with a specific input state
sel_run_at(t, T, with_input_state(Tool, State)) :-
    query(t),
    occurs(T, run(Tool)),
    occurs(T, bind(Tool, _, WF)),
    holds(T-1, dim(WF, State, state)).

% Tool with a specific output type
sel_run_at(t, T, with_output_type(Tool, TypeVal)) :-
    query(t),
    occurs(T, run(Tool)),
    occurs(T, output(Tool, _, Port)),
    holds(T, dim(out(T, Tool, Port), TypeVal, type)).

% Tool with a specific output state
sel_run_at(t, T, with_output_state(Tool, State)) :-
    query(t),
    occurs(T, run(Tool)),
    occurs(T, output(Tool, _, Port)),
    holds(T, dim(out(T, Tool, Port), State, state)).

% Tool with a specific input unit
sel_run_at(t, T, with_input_unit(Tool, Unit)) :-
    query(t),
    occurs(T, run(Tool)),
    occurs(T, bind(Tool, _, WF)),
    holds(T-1, unit(WF, Unit)).

% Tool with a given tool_property annotation
sel_run_at(t, T, with_property(Tool, Prop)) :-
    query(t),
    occurs(T, run(Tool)),
    tool_property(Tool, Prop).

% Tool with a specific requirement
sel_run_at(t, T, with_requirement(Tool, ReqType, ReqVal)) :-
    query(t),
    occurs(T, run(Tool)),
    tool_requirement(Tool, (ReqType, ReqVal)).

% Inline conjunction — only fires for and-selectors used in constraints
sel_run_at(t, T, and(SelA, SelB)) :-
    query(t),
    sel_used(and(SelA, SelB)),
    sel_run_at(t, T, SelA),
    sel_run_at(t, T, SelB).

% Named compound selector — every selector_cond must match at T
sel_run_at(t, T, Name) :-
    query(t),
    selector(Name),
    selector_cond(Name, _),
    occurs(T, run(_)),
    sel_run_at(t, T, Cond) : selector_cond(Name, Cond).

% ----------------------------------------------------------------
% ENFORCEMENT
% ----------------------------------------------------------------

% SelA at T, SelB at T+1 (directly adjacent steps)
:- query(t),
   before(SelA, SelB),
   sel_run_at(t, T1, SelA),
   sel_run_at(t, T2, SelB),
   T2 != T1 + 1.

% SelA at T, SelB at T-1 (directly adjacent, reversed)
:- query(t),
   after(SelA, SelB),
   sel_run_at(t, T1, SelA),
   sel_run_at(t, T2, SelB),
   T1 != T2 + 1.

% SelA strictly before SelB (any gap)
:- query(t),
   eventually_before(SelA, SelB),
   sel_run_at(t, T1, SelA),
   sel_run_at(t, T2, SelB),
   T1 >= T2.

% SelA strictly after SelB (any gap)
:- query(t),
   eventually_after(SelA, SelB),
   sel_run_at(t, T1, SelA),
   sel_run_at(t, T2, SelB),
   T1 <= T2.
