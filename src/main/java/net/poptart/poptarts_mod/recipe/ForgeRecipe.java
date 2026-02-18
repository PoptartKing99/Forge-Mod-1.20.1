package net.poptart.poptarts_mod.recipe;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.RecipeMatcher;


public class ForgeRecipe extends AbstractForgeRecipe {

    private final ItemStack output;
    private final NonNullList<Ingredient> recipeItems;
    private final int cookTime;


    public ForgeRecipe(ResourceLocation id, String group, ForgingBookCategory category, ItemStack output, NonNullList<Ingredient> recipeItems, int cookTime) {
        super(id, group, category, output, recipeItems, cookTime);
        this.output = output;
        this.recipeItems = recipeItems;
        this.cookTime = cookTime;

    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public boolean matches(Container container, Level level) {
        // Output slot rules
        ItemStack outputSlot = container.getItem(10);

        if (!outputSlot.isEmpty() && !ItemStack.isSameItemSameTags(outputSlot, this.output)) {
            return false;
        }

        if (!outputSlot.isEmpty() && outputSlot.getCount() >= outputSlot.getMaxStackSize()) {
            return false;
        }

        // Collect ALL input items (including duplicates)
        NonNullList<ItemStack> inputs = NonNullList.create();

        for (int i = 0; i < 9; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                inputs.add(stack.copyWithCount(1)); // only count 1 per slot per craft check
            }
        }

        // If number of filled slots doesn't match ingredients, fail
        if (inputs.size() != this.recipeItems.size()) {
            return false;
        }

        // Now do shapeless matching
        return RecipeMatcher.findMatches(inputs, this.recipeItems) != null;
    }


    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return this.output.copy();
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return this.output.copy();
    }
    @Override
    public boolean canCraftInDimensions(int p_43999_, int p_44000_) {
        return true;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.FORGING_TYPE.get();
    }


    public static class Serializer implements RecipeSerializer<ForgeRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        private static final ResourceLocation NAME = ResourceLocation.tryParse("poptarts_mod:forging");
        public ForgeRecipe fromJson(ResourceLocation resourceLocation, JsonObject json) {
            String group = GsonHelper.getAsString(json, "group", "");
            ForgingBookCategory category = ForgingBookCategory.CODEC.byName(GsonHelper.getAsString(json, "category", null));
            if (category == null) category = ForgingBookCategory.MISC;
            NonNullList<Ingredient> inputs = itemsFromJson(GsonHelper.getAsJsonArray(json, "ingredients"));
            if (inputs.isEmpty()) {
                throw new JsonParseException("No ingredients for forging recipe");
            } else if (inputs.size() > 9) {
                throw new JsonParseException("Too many ingredients for forging recipe. The maximum is 9");
            } else {
                ItemStack itemstack = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
                int cookTimeIn = GsonHelper.getAsInt(json, "cooktime", 200);
                return new ForgeRecipe(resourceLocation, group, category, itemstack, inputs,  cookTimeIn);
            }
        }


        private static NonNullList<Ingredient> itemsFromJson(JsonArray ingredientArray) {
            NonNullList<Ingredient> nonnulllist = NonNullList.create();

            for(int i = 0; i < ingredientArray.size(); ++i) {
                Ingredient ingredient = Ingredient.fromJson(ingredientArray.get(i));
                if (true || !ingredient.isEmpty()) {
                    nonnulllist.add(ingredient);
                }
            }
            return nonnulllist;
        }
        @Override
        public ForgeRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            String group = buf.readUtf();
            ForgingBookCategory category = buf.readEnum(ForgingBookCategory.class);
            int i = buf.readVarInt();
            NonNullList<Ingredient> inputs = NonNullList.withSize(i, Ingredient.EMPTY);

            for(int j = 0; j < inputs.size(); ++j) {
                inputs.set(j, Ingredient.fromNetwork(buf));
            }

            ItemStack itemstack = buf.readItem();
            int cookTimeIn = buf.readVarInt();
            return new ForgeRecipe(id, group, category, itemstack, inputs, cookTimeIn);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ForgeRecipe recipe) {
            buf.writeUtf(recipe.group);
            buf.writeEnum(recipe.category);
            buf.writeVarInt(recipe.recipeItems.size());
            for(Ingredient ingredient : recipe.getIngredients()) {ingredient.toNetwork(buf);}
            buf.writeItem(recipe.getResultItem());
            buf.writeVarInt(recipe.cookTime);

        }
    }
}