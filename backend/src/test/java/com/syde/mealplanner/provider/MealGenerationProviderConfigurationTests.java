package com.syde.mealplanner.provider;

import com.syde.mealplanner.config.MealGenerationConfig;
import com.syde.mealplanner.provider.impl.MockMealGenerationProvider;
import com.syde.mealplanner.provider.impl.OpenAiMealGenerationProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MealGenerationProviderConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    MealGenerationConfig.class,
                    MockMealGenerationProvider.class,
                    OpenAiMealGenerationProvider.class);

    @Test
    void defaultConfigurationSelectsMockProviderWithoutOpenAiApiKey() {
        contextRunner.run(context -> {
            MealGenerationProvider provider = context.getBean(MealGenerationProvider.class);

            assertNotNull(provider);
            assertInstanceOf(MockMealGenerationProvider.class, provider);
        });
    }

    @Test
    void openAiProviderRequiresApiKeyWhenSelected() {
        contextRunner
                .withPropertyValues("meal-generation.provider=openai")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void openAiProviderCanStartWithDummyApiKey() {
        contextRunner
                .withPropertyValues(
                        "meal-generation.provider=openai",
                        "meal-generation.openai.api-key=test-api-key")
                .run(context -> {
                    MealGenerationProvider provider = context.getBean(MealGenerationProvider.class);

                    assertNotNull(provider);
                    assertInstanceOf(OpenAiMealGenerationProvider.class, provider);
                });
    }
}
