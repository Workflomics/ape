% ================================================================
% ARTIFACT STATE CONSTRAINTS (multi-shot)
% #program check(t)
%
% Activation: require_state/reject_state/require_state_at in config
% Docs: DOCS/multi_shot/constraints/artifact_state_constraints.md
%
% Checks that artifact state fluents hold (or don't hold) at some
% or specific timesteps within the committed plan horizon.
% ================================================================

#program check(t).

% ----------------------------------------------------------------
% require_state(Artifact, State) — state must hold at some point
% ----------------------------------------------------------------

:- query(t),
   require_state(Artifact, State),
   #count { T : holds(T, state(Artifact, State)) } = 0.

% ----------------------------------------------------------------
% reject_state(Artifact, State) — state must never hold
% ----------------------------------------------------------------

:- query(t),
   reject_state(Artifact, State),
   holds(_, state(Artifact, State)).

% ----------------------------------------------------------------
% require_state_at(Ts, Artifact, State) — state must hold at specific step
% ----------------------------------------------------------------

:- query(t),
   require_state_at(Ts, Artifact, State),
   Ts <= t,
   not holds(Ts, state(Artifact, State)).
