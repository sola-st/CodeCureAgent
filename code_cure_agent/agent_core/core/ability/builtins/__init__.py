from agent_core.core.ability.builtins.create_new_ability import CreateNewAbility
from agent_core.core.ability.builtins.query_language_model import QueryLanguageModel

BUILTIN_ABILITIES = {
    QueryLanguageModel.name(): QueryLanguageModel,
}
