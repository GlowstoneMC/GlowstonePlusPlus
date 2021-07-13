package net.glowstone.inventory;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.KnowledgeBookMeta;
import org.jetbrains.annotations.NotNull;

public class GlowMetaKnowledgeBook extends GlowMetaItem implements KnowledgeBookMeta {

    private final List<NamespacedKey> recipes = new ArrayList<>();

    /**
     * Creates an instance by copying from the given {@link ItemMeta}. If that item is another
     * {@link KnowledgeBookMeta}, its recipes are copied; otherwise, the new book is empty.
     * @param meta the {@link ItemMeta} to copy
     */
    public GlowMetaKnowledgeBook(ItemMeta meta) {
        super(meta);

        if (!(meta instanceof KnowledgeBookMeta)) {
            return;
        }

        KnowledgeBookMeta book = (KnowledgeBookMeta) meta;
        if (book.hasRecipes()) {
            recipes.addAll(book instanceof GlowMetaKnowledgeBook
                    ? ((GlowMetaKnowledgeBook) book).recipes : book.getRecipes());
        }
    }

    @Override
    public boolean hasRecipes() {
        return !recipes.isEmpty();
    }

    @Override
    public @NotNull List<NamespacedKey> getRecipes() {
        return ImmutableList.copyOf(recipes);
    }

    @Override
    public void setRecipes(@NotNull List<NamespacedKey> recipes) {
        this.recipes.clear();
        this.recipes.addAll(recipes);
    }

    @Override
    public void addRecipe(NamespacedKey... recipes) {
        this.recipes.addAll(Arrays.asList(recipes));
    }

    @Override
    public @NotNull KnowledgeBookMeta clone() {
        return new GlowMetaKnowledgeBook(this);
    }
}
