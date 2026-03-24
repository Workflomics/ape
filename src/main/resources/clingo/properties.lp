% ================================================================
% PIPELINE PROPERTY CONSTRAINTS (multi-shot)
% #program check(t)
%
% Activation: require_property(Prop). / reject_property(Prop). in config
% Docs: DOCS/multi_shot/constraints/properties.md
%
% pipeline_has(Prop) is not used in the multi-shot encoding (it is a
% non-timestamped atom that causes redefinition errors across step
% groundings). Instead, step.lp derives pipeline_has_at(T, Prop) per
% step. This module accumulates those into pipeline_has_check(t, Prop)
% at the current horizon and enforces require/reject constraints.
% ================================================================

#program check(t).

% ----------------------------------------------------------------
% Accumulate all properties across all steps up to horizon t
% ----------------------------------------------------------------

pipeline_has_check(t, Prop) :-
    query(t),
    pipeline_has_at(T, Prop),
    T >= 1.

% ----------------------------------------------------------------
% require_property — the plan must include the property somewhere
% ----------------------------------------------------------------

:- query(t), require_property(Prop), not pipeline_has_check(t, Prop).

% ----------------------------------------------------------------
% reject_property — the plan must not include the property anywhere
% ----------------------------------------------------------------

:- query(t), reject_property(Prop), pipeline_has_check(t, Prop).
