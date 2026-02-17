"""
LLM interaction logging utilities for tracking prompts, responses, and costs.
"""

import json
import os
from datetime import datetime

# Model pricing structure (costs per 1K tokens)
# Structure: (uncached_input_cost, cached_input_cost, output_cost)
MODEL_PRICING = {
    "openai/gpt-4o": (0.0025, 0.00125, 0.010),
    "openai/gpt-4.1-mini": (0.0004, 0.0001, 0.0016),
    "openai/gpt-4.1": (0.002, 0.0005, 0.008),
    "openai/gpt-5-mini": (0.00025, 0.000025, 0.002),
    "openai/gpt-5": (0.00125, 0.000125, 0.01),
    "openai/gpt-5.1": (0.00125, 0.000125, 0.010)
}

def get_model_pricing(model_name):
    """Get pricing for a specific model, fallback to gpt-4.1-mini if not found."""
    if model_name.startswith("gpt-4.1-mini"):
        model_key = "openai/gpt-4.1-mini"
    elif model_name.startswith("gpt-4o"):
        model_key = "openai/gpt-4o"
    elif model_name.startswith("gpt-4.1"):
        model_key = "openai/gpt-4.1"
    elif model_name.startswith("gpt-5-mini"):
        model_key = "openai/gpt-5-mini"
    elif model_name.startswith("gpt-5.1"):
        model_key = "openai/gpt-5.1"
    elif model_name.startswith("gpt-5"):
        model_key = "openai/gpt-5"
    else:
        print(f"Unknown model name '{model_name}', defaulting to gpt-4.1-mini pricing.")
        model_key = "openai/gpt-4.1-mini"
    
    return MODEL_PRICING.get(model_key, MODEL_PRICING["openai/gpt-4.1-mini"])

def log_llm_interaction(warning_id, interaction_type, prompt, response_content, usage, output_dir, model_name="gpt-4.1-mini-2025-04-14"):
    """Log LLM interaction with prompt, response, and token usage."""
    # Get model-specific pricing
    uncached_input_cost_per_1k, cached_input_cost_per_1k, output_cost_per_1k = get_model_pricing(model_name)
    
    # Calculate costs based on cached vs uncached tokens
    cached_tokens = getattr(usage.prompt_tokens_details, 'cached_tokens', 0) if usage and hasattr(usage, 'prompt_tokens_details') else 0
    uncached_tokens = (usage.prompt_tokens - cached_tokens) if usage else 0
    
    input_cost = (uncached_tokens * uncached_input_cost_per_1k / 1000) + (cached_tokens * cached_input_cost_per_1k / 1000) if usage else 0
    output_cost = (usage.completion_tokens * output_cost_per_1k / 1000) if usage else 0
    
    log_data = {
        "timestamp": datetime.now().isoformat(),
        "warning_id": warning_id,
        "interaction_type": interaction_type,  # "example_generation" or "refactoring"
        "model_name": model_name,
        "prompt": prompt,
        "response": response_content,
        "token_usage": {
            "prompt_tokens": usage.prompt_tokens if usage else 0,
            "completion_tokens": usage.completion_tokens if usage else 0,
            "total_tokens": usage.total_tokens if usage else 0,
            "prompt_tokens_details": {
                "cached_tokens": cached_tokens,
                "uncached_tokens": uncached_tokens
            }
        },
        "estimated_cost": {
            "input_cost": input_cost,
            "output_cost": output_cost,
            "total_cost": input_cost + output_cost,
            "pricing_used": {
                "uncached_input_cost_per_1k": uncached_input_cost_per_1k,
                "cached_input_cost_per_1k": cached_input_cost_per_1k,
                "output_cost_per_1k": output_cost_per_1k
            }
        }
    }
    
    os.makedirs(output_dir, exist_ok=True)
    
    log_file = os.path.join(output_dir, f"llm_interactions_log.json")
    
    # Read existing logs or create empty list
    existing_logs = []
    if os.path.exists(log_file):
        try:
            with open(log_file, 'r', encoding='utf-8') as f:
                existing_logs = json.load(f)
        except (json.JSONDecodeError, FileNotFoundError):
            existing_logs = []
    
    existing_logs.append(log_data)
    
    with open(log_file, 'w', encoding='utf-8') as f:
        json.dump(existing_logs, f, indent=2, ensure_ascii=False)