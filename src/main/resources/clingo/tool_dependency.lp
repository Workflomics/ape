% ================================================================
% TOOL DEPENDENCY / EXCLUSION CONSTRAINTS (multi-shot)
% #program check(t)
%
% Activation: requires(A, B). / excludes(A, B). in config / extra_lp
% Docs: DOCS/multi_shot/constraints/tool_dependency.md
%
% All constraints live in #program check(t) with a query(t) guard.
% ================================================================

#program check(t).

% ----------------------------------------------------------------
% requires(A, B) — if A runs, B must also run (concrete tools)
% ----------------------------------------------------------------

:- query(t),
   requires(ToolA, ToolB),
   not tool_abstract_class(ToolA),
   not tool_abstract_class(ToolB),
   occurs(_, run(ToolA)),
   #count { T : occurs(T, run(ToolB)) } = 0.

% ----------------------------------------------------------------
% excludes(A, B) — A and B cannot both run (concrete tools)
% ----------------------------------------------------------------

:- query(t),
   excludes(ToolA, ToolB),
   not tool_abstract_class(ToolA),
   not tool_abstract_class(ToolB),
   occurs(_, run(ToolA)),
   occurs(_, run(ToolB)).

% ----------------------------------------------------------------
% Abstract requires — A is abstract class: any member running requires B
% ----------------------------------------------------------------

:- query(t),
   requires(C1, C2),
   tool_abstract_class(C1),
   not tool_abstract_class(C2),
   occurs(_, run(Tool1)), tool_is_a(Tool1, C1),
   #count { T : occurs(T, run(C2)) } = 0.

:- query(t),
   requires(C1, C2),
   not tool_abstract_class(C1),
   tool_abstract_class(C2),
   occurs(_, run(C1)),
   #count { T : tool_is_a(Tool2, C2), tool(Tool2), occurs(T, run(Tool2)) } = 0.

:- query(t),
   requires(C1, C2),
   tool_abstract_class(C1),
   tool_abstract_class(C2),
   occurs(_, run(Tool1)), tool_is_a(Tool1, C1),
   #count { T : tool_is_a(Tool2, C2), tool(Tool2), occurs(T, run(Tool2)) } = 0.

% ----------------------------------------------------------------
% Abstract excludes — A is abstract class: any member running excludes B
% ----------------------------------------------------------------

:- query(t),
   excludes(C1, C2),
   tool_abstract_class(C1),
   not tool_abstract_class(C2),
   occurs(_, run(Tool1)), tool_is_a(Tool1, C1),
   occurs(_, run(C2)).

:- query(t),
   excludes(C1, C2),
   not tool_abstract_class(C1),
   tool_abstract_class(C2),
   occurs(_, run(C1)),
   occurs(_, run(Tool2)), tool_is_a(Tool2, C2).

:- query(t),
   excludes(C1, C2),
   tool_abstract_class(C1),
   tool_abstract_class(C2),
   occurs(_, run(Tool1)), tool_is_a(Tool1, C1),
   occurs(_, run(Tool2)), tool_is_a(Tool2, C2).
