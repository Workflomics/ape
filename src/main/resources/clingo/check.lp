#program check(t).
#external query(t).

% ================================================================
% CHECK — volatile goal and usefulness check at horizon t
% ================================================================

% Hard goal constraint
:- query(t), not holds(t, goal).

% Exact-horizon mode: goal must be achieved for the FIRST TIME at step t.
% Declare exact_horizon_mode. in your config to activate.
:- query(t), exact_horizon_mode, t > 1, holds(t-1, goal).

% ----------------------------------------------------------------
% Usefulness: full backward propagation at check time
% Namespaced as useful_check(t, T, Tool) to avoid stale atoms
% from previous check iterations polluting the model.
% ----------------------------------------------------------------

% Direct — output satisfies goal at current horizon
useful_check(t, T, Tool) :-
    query(t),
    occurs(T, run(Tool)),
    occurs(T, output(Tool, _, Port)),
    goal_satisfied_by(t, _, out(T, Tool, Port)).

% Backward — output consumed by a useful tool
useful_check(t, T1, Tool1) :-
    query(t),
    occurs(T1, run(Tool1)),
    occurs(T1, output(Tool1, _, Port)),
    occurs(T2, bind(Tool2, _, out(T1, Tool1, Port))),
    useful_check(t, T2, Tool2),
    T2 > T1.

% Trivial — tools with no output
useful_check(t, T, Tool) :-
    query(t),
    occurs(T, run(Tool)),
    not tool_has_output(Tool).

% Constraint: every tool that runs must be useful
:- query(t), occurs(T, run(Tool)), not useful_check(t, T, Tool).

% Self-loop prevention
:- query(t), occurs(T2, bind(Tool, _, out(T1, Tool, _))), T2 > T1.
