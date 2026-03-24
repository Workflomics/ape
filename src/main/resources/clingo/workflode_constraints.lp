% ================================================================
% WORKFLODE CONSTRAINTS (multi-shot)
% #program check(t)
%
% Activation: use_workflode(W). in config / extra_lp
% Docs: DOCS/multi_shot/constraints/workflode_constraints.md
% ================================================================

#program check(t).

% ----------------------------------------------------------------
% Step presence — every workflode step must run at least once
% ----------------------------------------------------------------

:- query(t),
   use_workflode(W),
   workflode_step(W, Tool),
   #count { T : occurs(T, run(Tool)) } = 0.

% ----------------------------------------------------------------
% Parallel co-scheduling — workflode step must run in parallel
% with at least one other tool
% ----------------------------------------------------------------

:- query(t),
   use_workflode(W),
   workflode_step(W, Tool),
   occurs(Ts, run(Tool)),
   #count { Other : occurs(Ts, run(Other)), Other != Tool } = 0.

% ----------------------------------------------------------------
% Join point — all workflode steps must complete before JoinTool
% ----------------------------------------------------------------

:- query(t),
   use_workflode(W),
   workflode_step(W, Step),
   workflode_join(W, JoinTool),
   occurs(T1, run(Step)),
   occurs(T2, run(JoinTool)),
   T1 >= T2.
