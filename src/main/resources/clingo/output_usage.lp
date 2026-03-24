% ================================================================
% OUTPUT USAGE CONSTRAINTS (multi-shot)
% #program check(t)
%
% Activation: enable_all_outputs_consumed.   (use_all_generated_data = ALL)
%             enable_primary_output_consumed. (use_all_generated_data = ONE)
%
% Mirrors SAT's EnforceModuleRelatedRules.dataUsageConstraints():
%   ALL  — every tool output must be consumed by a subsequent bind OR satisfy the goal
%   ONE  — for each tool step, at least one output is consumed OR the goal is satisfied
% ================================================================

#program check(t).

% ----------------------------------------------------------------
% enable_all_outputs_consumed — every tool output consumed or goal
% ----------------------------------------------------------------

:- query(t),
   enable_all_outputs_consumed,
   occurs(T, output(Tool, _, Port)),
   not goal_satisfied_by(t, _, out(T, Tool, Port)),
   #count { T2, Tool2, Port2 : occurs(T2, bind(Tool2, Port2, out(T, Tool, Port))) } = 0.

% ----------------------------------------------------------------
% enable_primary_output_consumed — at least one output per step used
% ----------------------------------------------------------------

:- query(t),
   enable_primary_output_consumed,
   occurs(T, run(Tool)),
   tool_has_output(Tool),
   % no output of this tool-run reaches any subsequent bind
   #count { Port, T2, Tool2, Port2 :
       occurs(T, output(Tool, _, Port)),
       occurs(T2, bind(Tool2, Port2, out(T, Tool, Port))) } = 0,
   % and no output of this tool-run satisfies the goal
   #count { Port : occurs(T, output(Tool, _, Port)),
                   goal_satisfied_by(t, _, out(T, Tool, Port)) } = 0.
