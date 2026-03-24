% ================================================================
% ENERGY COST (multi-shot)
% #program check(t)
%
% Activation: max_plan_energy(MaxCost). in config / extra_lp
% Docs: DOCS/multi_shot/constraints/energy_cost.md
%
% All computation is namespaced with t to prevent redefinition
% errors. effective_energy_cost(T, Tool, Cost) from single-shot
% becomes effective_energy_cost_check(t, T, Tool, Cost) here.
%
% #minimize is not supported in this version — multi-shot
% optimization requires special driver support (use_opt_mode).
% ================================================================

#program check(t).

% ----------------------------------------------------------------
% Input-specific cost contributions
% ----------------------------------------------------------------

energy_cost_contribution_check(t, T, Tool, DimCat, DimVal, Cost) :-
    query(t),
    occurs(T, run(Tool)),
    occurs(T, bind(Tool, _, WF)),
    holds(T-1, dim(WF, DimVal, DimCat)),
    tool_input_energy_cost(Tool, DimCat, DimVal, Cost).

% ----------------------------------------------------------------
% Effective cost per tool run = base + sum of input contributions
% ----------------------------------------------------------------

effective_energy_cost_check(t, T, Tool, TotalCost) :-
    query(t),
    occurs(T, run(Tool)),
    tool_energy_cost(Tool, BaseCost),
    InputCost = #sum { Cost, DimCat, DimVal :
                       energy_cost_contribution_check(t, T, Tool, DimCat, DimVal, Cost) },
    TotalCost = BaseCost + InputCost.

effective_energy_cost_check(t, T, Tool, 0) :-
    query(t),
    occurs(T, run(Tool)),
    not tool_energy_cost(Tool, _).

% ----------------------------------------------------------------
% Plan total energy at current horizon
% ----------------------------------------------------------------

plan_energy_cost_check(t, TotalCost) :-
    query(t),
    TotalCost = #sum { Cost, T, Tool : effective_energy_cost_check(t, T, Tool, Cost) }.

% ----------------------------------------------------------------
% Energy budget constraint
% ----------------------------------------------------------------

:- query(t), max_plan_energy(Max), plan_energy_cost_check(t, Cost), Cost > Max.
