package net.poptart.poptarts_mod.recipe;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Set;

public class ForgeShapedRecipe extends AbstractForgeRecipe implements IShapedRecipe<Container> {
    static int MAX_WIDTH = 3;
    static int MAX_HEIGHT = 3;

    public static void setCraftingSize(int width, int height) {
        if (MAX_WIDTH < width) MAX_WIDTH = width;
        if (MAX_HEIGHT < height) MAX_HEIGHT = height;
    }

    final int width;
    final int height;
    private final ItemStack output;
    private final NonNullList<Ingredient> recipeItems;
    private final int cookTime;
    private final boolean isSimple;

    public ForgeShapedRecipe(int width, int height, ResourceLocation id, String group, ForgingBookCategory category, ItemStack output, NonNullList<Ingredient> recipeItems, int cookTime) {
        super(id, group, category, output, recipeItems, cookTime);
        this.width = width;
        this.height = height;
        this.output = output;
        this.recipeItems = recipeItems;
        this.cookTime = cookTime;
        this.isSimple = recipeItems.stream().allMatch(Ingredient::isSimple);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public boolean matches(Container pContainer, Level pLevel) {
        for (int width = 0; width <= 3 - this.width; ++width) {
            for (int height = 0; height <= 3 - this.height; ++height) {
                if (this.matches(pContainer, width, height, true)) return true;
                if (this.matches(pContainer, width, height, false)) return true;
            }
        }
        return false;
    }

    private boolean matches(Container pContainer, int width, int height, boolean mirrored) {
        for (int xn = 0; xn < 3; ++xn) {
            for (int yn = 0; yn < 3; ++yn) {
                int x = xn - width;
                int y = yn - height;
                Ingredient ingredient = Ingredient.EMPTY;
                if (x >= 0 && y >= 0 && x < this.width && y < this.height) {
                    ingredient = mirrored
                            ? this.recipeItems.get(this.width - x - 1 + y * this.width)
                            : this.recipeItems.get(x + y * this.width);
                }
                if (!ingredient.test(pContainer.getItem(xn + yn * 3))) return false;
            }
        }
        return true;
    }

    // --- Updated assemble methods for 1.20.1 ---
    @Override
    public ItemStack assemble(Container container, net.minecraft.core.RegistryAccess registryAccess) {
        return output.copy();
    }

    @Override
    public ItemStack getResultItem(net.minecraft.core.RegistryAccess registryAccess) {
        return output.copy();
    }

    // --- IShapedRecipe methods ---

    public int getWidth() { return this.width; }

    public int getHeight() { return this.height; }

    public int getRecipeWidth() { return getWidth(); }

    public int getRecipeHeight() { return getHeight(); }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return true; }

    @Override
    public RecipeType<?> getType() { return ModRecipes.FORGING_TYPE.get(); }

    // --- Pattern and key parsing helpers ---
    static NonNullList<Ingredient> dissolvePattern(String[] pattern, Map<String, Ingredient> key, int width, int height) {
        NonNullList<Ingredient> result = NonNullList.withSize(width * height, Ingredient.EMPTY);
        Set<String> unused = Sets.newHashSet(key.keySet());
        unused.remove(" ");

        for (int y = 0; y < pattern.length; y++) {
            for (int x = 0; x < pattern[y].length(); x++) {
                String s = pattern[y].substring(x, x + 1);
                Ingredient ingredient = key.get(s);
                if (ingredient == null)
                    throw new JsonSyntaxException("Pattern references symbol '" + s + "' but it's not defined in the key");
                unused.remove(s);
                result.set(x + width * y, ingredient);
            }
        }

        if (!unused.isEmpty()) throw new JsonSyntaxException("Key defines symbols that aren't used in pattern: " + unused);
        return result;
    }

    @VisibleForTesting
    static String[] shrink(String... pattern) {
        int minX = Integer.MAX_VALUE, maxX = 0, emptyTop = 0, emptyBottom = 0;
        for (int i = 0; i < pattern.length; ++i) {
            String row = pattern[i];
            minX = Math.min(minX, firstNonSpace(row));
            maxX = Math.max(maxX, lastNonSpace(row));
            if (lastNonSpace(row) < 0) emptyTop = (emptyTop == i ? emptyTop + 1 : emptyTop); else emptyBottom = 0;
        }
        if (pattern.length == emptyBottom) return new String[0];
        String[] out = new String[pattern.length - emptyBottom - emptyTop];
        for (int i = 0; i < out.length; i++) out[i] = pattern[i + emptyTop].substring(minX, maxX + 1);
        return out;
    }

    private static int firstNonSpace(String s) {
        int i = 0; while (i < s.length() && s.charAt(i) == ' ') i++; return i;
    }

    private static int lastNonSpace(String s) {
        int i = s.length() - 1; while (i >= 0 && s.charAt(i) == ' ') i--; return i;
    }

    static String[] patternFromJson(JsonArray array) {
        String[] result = new String[array.size()];
        if (result.length > MAX_HEIGHT) throw new JsonSyntaxException("Invalid pattern: too many rows, " + MAX_HEIGHT + " is maximum");
        if (result.length == 0) throw new JsonSyntaxException("Invalid pattern: empty pattern not allowed");
        for (int i = 0; i < result.length; i++) {
            String s = GsonHelper.convertToString(array.get(i), "pattern[" + i + "]");
            if (s.length() > MAX_WIDTH) throw new JsonSyntaxException("Invalid pattern: too many columns, " + MAX_WIDTH + " is maximum");
            if (i > 0 && result[0].length() != s.length()) throw new JsonSyntaxException("Invalid pattern: each row must be the same width");
            result[i] = s;
        }
        return result;
    }

    static Map<String, Ingredient> keyFromJson(JsonObject obj) {
        Map<String, Ingredient> map = Maps.newHashMap();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            if (entry.getKey().length() != 1) throw new JsonSyntaxException("Invalid key entry: '" + entry.getKey() + "' is an invalid symbol (must be 1 character only).");
            if (" ".equals(entry.getKey())) throw new JsonSyntaxException("Invalid key entry: ' ' is a reserved symbol.");
            map.put(entry.getKey(), Ingredient.fromJson(entry.getValue()));
        }
        map.put(" ", Ingredient.EMPTY);
        return map;
    }

    public static ItemStack itemStackFromJson(JsonObject obj) {
        return net.minecraftforge.common.crafting.CraftingHelper.getItemStack(obj, true, true);
    }

    public static Item itemFromJson(JsonObject obj) {
        String s = GsonHelper.getAsString(obj, "item");
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(s));
        if (item == null || item == Items.AIR) {
            throw new JsonSyntaxException("Unknown item: " + s);
        }
        return item;
    }

    // --- Serializer ---
    public static class Serializer implements RecipeSerializer<ForgeShapedRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        private static final ResourceLocation NAME = ResourceLocation.tryParse("poptarts_mod:forging_shaped");

        public ForgeShapedRecipe fromJson(ResourceLocation id, JsonObject json) {
            String group = GsonHelper.getAsString(json, "group", "");
            ForgingBookCategory category = ForgingBookCategory.CODEC.byName(GsonHelper.getAsString(json, "category", null));
            if (category == null) category = ForgingBookCategory.MISC;

            Map<String, Ingredient> key = keyFromJson(GsonHelper.getAsJsonObject(json, "key"));
            String[] pattern = shrink(patternFromJson(GsonHelper.getAsJsonArray(json, "pattern")));
            NonNullList<Ingredient> ingredients = dissolvePattern(pattern, key, pattern[0].length(), pattern.length);
            ItemStack result = itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int cookTime = GsonHelper.getAsInt(json, "cooktime", 200);
            return new ForgeShapedRecipe(pattern[0].length(), pattern.length, id, group, category, result, ingredients, cookTime);
        }

        @Override
        public ForgeShapedRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            int width = buf.readVarInt();
            int height = buf.readVarInt();
            String group = buf.readUtf();
            ForgingBookCategory category = buf.readEnum(ForgingBookCategory.class);
            NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
            for (int i = 0; i < ingredients.size(); i++) ingredients.set(i, Ingredient.fromNetwork(buf));
            ItemStack result = buf.readItem();
            int cookTime = buf.readVarInt();
            return new ForgeShapedRecipe(width, height, id, group, category, result, ingredients, cookTime);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ForgeShapedRecipe recipe) {
            buf.writeVarInt(recipe.width);
            buf.writeVarInt(recipe.height);
            buf.writeUtf(recipe.group);
            buf.writeEnum(recipe.category);
            for (Ingredient ing : recipe.recipeItems) ing.toNetwork(buf);
            buf.writeItem(recipe.getResultItem(net.minecraft.core.RegistryAccess.EMPTY));
            buf.writeVarInt(recipe.cookTime);
        }
    }
}