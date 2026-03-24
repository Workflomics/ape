% ================================================================
% TOOL INCLUSION / EXCLUSION CONSTRAINTS (multi-shot)
% #program check(t)
%
% Activation: must_use(Tool). / must_not_use(Tool). in config / extra_lp
% Docs: DOCS/multi_shot/constraints/tool_inclusion.md
%
% All constraints live in #program check(t) with a query(t) guard.
% Using occurs(T, run(Tool)) ranges over ALL steps committed so far;
% the query(t) guard namespaces the constraint to the current horizon.
% ================================================================

#program check(t).

% ----------------------------------------------------------------
% must_use(Tool) — concrete tool must run at least once
% ----------------------------------------------------------------

:- query(t),
   must_use(Tool),
   not tool_abstract_class(Tool),
   #count { T : occurs(T, run(Tool)) } = 0.

% ----------------------------------------------------------------
% must_not_use(Tool) — concrete tool must not run at all
% ----------------------------------------------------------------

:- query(t),
   must_not_use(Tool),
   not tool_abstract_class(Tool),
   occurs(_, run(Tool)).

% ----------------------------------------------------------------
% must_use(Class) — at least one member of the abstract class runs
% ----------------------------------------------------------------

:- query(t),
   must_use(Class),
   tool_abstract_class(Class),
   #count { Tool : tool_is_a(Tool, Class), occurs(_, run(Tool)) } = 0.

% ----------------------------------------------------------------
% must_not_use(Class) — no member of the abstract class runs
% ----------------------------------------------------------------

:- query(t),
   must_not_use(Class),
   tool_abstract_class(Class),
   tool_is_a(Tool, Class),
   tool(Tool),
   occurs(_, run(Tool)).
