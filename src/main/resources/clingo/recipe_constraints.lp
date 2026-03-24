% ================================================================
% RECIPE CONSTRAINTS (multi-shot)
% #program base
%
% Item injection and constraint delegation for named recipe templates.
% Docs: DOCS/multi_shot/constraints/recipe_constraints.md
%
% This module is entirely in #program base — recipe expansion produces
% holds(0,...) facts and static must_use/require_property derivations,
% all of which are static and do not vary across step groundings.
% ================================================================

#program base.

% ----------------------------------------------------------------
% Item injection — inject recipe ingredients into the initial state
% ----------------------------------------------------------------

holds(0, avail(recipe_item(R, Idx))) :-
    use_recipe(R),
    recipe_item(R, Idx, _, _).

holds(0, dim(recipe_item(R, Idx), Val, Cat)) :-
    use_recipe(R),
    recipe_item(R, Idx, Cat, Val).

holds(0, unit(recipe_item(R, Idx), Unit)) :-
    use_recipe(R),
    recipe_item_unit(R, Idx, Unit).

% ----------------------------------------------------------------
% Constraint delegation — expand into existing constraint mechanisms
% ----------------------------------------------------------------

must_use(Tool)         :- use_recipe(R), recipe_requires_tool(R, Tool).
require_property(Prop) :- use_recipe(R), recipe_requires_property(R, Prop).
