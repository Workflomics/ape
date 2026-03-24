% ================================================================
% INPUT USAGE CONSTRAINTS (multi-shot)
% #program check(t)
%
% Activation: enable_all_inputs_used. / enable_inputs_used_once.
%             enable_inputs_consumed. in config / extra_lp
% Docs: DOCS/multi_shot/constraints/input_usage.md
%
% Unlike the single-shot version, the intermediate input_used(WF)
% atom is not used here — it would cause redefinition errors when
% a new binding for WF first appears at step T > 1 (the atom was
% committed as false after earlier steps). Aggregate counts in the
% constraint body avoid this.
% ================================================================

#program check(t).

% ----------------------------------------------------------------
% enable_all_inputs_used — every initial ingredient must be bound
% ----------------------------------------------------------------

:- query(t),
   enable_all_inputs_used,
   holds(0, avail(WF)),
   #count { T, Tool, Port : occurs(T, bind(Tool, Port, WF)) } = 0.

% ----------------------------------------------------------------
% enable_inputs_used_once — each ingredient bound at most once
% ----------------------------------------------------------------

:- query(t),
   enable_inputs_used_once,
   holds(0, avail(WF)),
   #count { T, Tool, Port : occurs(T, bind(Tool, Port, WF)) } > 1.

% ----------------------------------------------------------------
% enable_inputs_consumed — synonym for used_once in multi-shot
% (consumed semantics: ingredient used at most once, same constraint)
% ----------------------------------------------------------------

:- query(t),
   enable_inputs_consumed,
   holds(0, avail(WF)),
   #count { T, Tool, Port : occurs(T, bind(Tool, Port, WF)) } > 1.
