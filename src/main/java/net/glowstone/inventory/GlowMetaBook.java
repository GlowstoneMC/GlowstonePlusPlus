package net.glowstone.inventory;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import net.glowstone.util.nbt.CompoundTag;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The ItemMeta for book and quill and written book items.
 */
class GlowMetaBook extends GlowMetaItem implements BookMeta {

    @Getter
    private String title;
    @Getter
    @Setter
    private String author;
    private List<String> pages;
    private Integer generation;

    /**
     * Creates an instance by copying from the given {@link ItemMeta}. If that item is another
     * {@link BookMeta}, its title, author, pages and generation are copied; otherwise, the new book
     * is blank.
     *
     * @param meta the {@link ItemMeta} to copy
     */
    public GlowMetaBook(ItemMeta meta) {
        super(meta);
        if (!(meta instanceof BookMeta)) {
            return;
        }
        BookMeta book = (BookMeta) meta;
        title = book.getTitle();
        author = book.getAuthor();
        if (book.hasPages()) {
            pages = new ArrayList<>(book instanceof GlowMetaBook
                    ? ((GlowMetaBook) book).pages : book.getPages());
            filterPages();
        }
        if (hasGeneration()) {
            this.generation = book.getGeneration().ordinal();
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Internal stuff

    @Override
    public BookMeta.@NotNull Spigot spigot() {
        return new BookMeta.Spigot() {
        };
    }

    @Override
    public @NotNull BookMeta clone() {
        return new GlowMetaBook(this);
    }

    @Override
    public boolean isApplicable(Material material) {
        return material == Material.WRITABLE_BOOK || material == Material.WRITTEN_BOOK;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> result = super.serialize();
        result.put("meta-type", "BOOK");
        if (hasAuthor()) {
            result.put("author", author);
        }
        if (hasTitle()) {
            result.put("title", title);
        }
        if (hasPages()) {
            result.put("pages", pages);
        }
        return result;
    }

    @Override
    void writeNbt(CompoundTag tag) {
        super.writeNbt(tag);
        if (hasAuthor()) {
            tag.putString("author", author);
        }
        if (hasTitle()) {
            tag.putString("title", title);
        }
        if (hasPages()) {
            tag.putStringList("pages", pages);
        }
        if (hasGeneration()) {
            tag.putInt("generation", generation);
        }
    }

    @Override
    void readNbt(CompoundTag tag) {
        super.readNbt(tag);
        tag.readString("author", this::setAuthor);
        tag.readString("title", this::setTitle);
        tag.readStringList("pages", pages -> {
            this.pages = pages;
            filterPages();
        });
        tag.readInt("generation", x -> generation = x);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties

    @Override
    public boolean hasTitle() {
        return title != null && !title.isEmpty();
    }

    @Override
    public boolean setTitle(String title) {
        if (title != null && title.length() > 16) {
            title = title.substring(0, 16);
        }
        this.title = title;
        return true;
    }

    @Override
    public boolean hasAuthor() {
        return author != null && !author.isEmpty();
    }

    @Override
    public boolean hasGeneration() {
        return generation != null;
    }

    @Override
    public Generation getGeneration() {
        if (generation == null) {
            return null;
        }
        return Generation.values()[generation];
    }

    @Override
    public void setGeneration(Generation generation) {
        if (generation == null) {
            this.generation = null;
            return;
        }
        this.generation = generation.ordinal();
    }

    @Override
    public boolean hasPages() {
        return pages != null && !pages.isEmpty();
    }

    @Override
    public @Nullable Component title() {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");

    }

    @Override
    public @NotNull BookMeta title(@Nullable Component component) {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public @Nullable Component author() {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public @NotNull BookMeta author(@Nullable Component component) {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public @NonNull List<Component> pages() {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public @NonNull Book pages(@NonNull List<Component> pages) {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public @NotNull Component page(int i) {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public void page(int i, @NotNull Component component) {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public void addPages(@NotNull Component... components) {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public @NonNull BookMetaBuilder toBuilder() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull String getPage(int page) {
        return pages.get(page);
    }

    @Override
    public void setPage(int page, @NotNull String data) {
        int size = getPageCount();
        if (page < 0 || page > size) {
            throw new IndexOutOfBoundsException("cannot set page " + page + " on size " + size);
        }
        if (!hasPages()) {
            pages = new ArrayList<>();
        }
        if (page == size) {
            pages.add(data);
        } else {
            pages.set(page, data);
        }
        filterPages();
    }

    @Override
    public @NotNull List<String> getPages() {
        return ImmutableList.copyOf(pages);
    }

    @Override
    public void setPages(String... pages) {
        this.pages = new ArrayList<>(Arrays.asList(pages));
        filterPages();
    }

    @Override
    public void setPages(@NotNull List<String> pages) {
        this.pages = new ArrayList<>(pages);
        filterPages();
    }

    @Override
    public void addPage(String... pages) {
        if (!hasPages()) {
            this.pages = new ArrayList<>();
        }
        this.pages.addAll(Arrays.asList(pages));
        filterPages();
    }

    @Override
    public int getPageCount() {
        return hasPages() ? pages.size() : 0;
    }

    /**
     * Filter the page list to 50 pages and 256 characters per page.
     */
    private void filterPages() {
        while (pages.size() > 50) {
            pages.remove(50);
        }
        for (int i = 0; i < pages.size(); ++i) {
            String page = pages.get(i);
            if (page.length() > 256) {
                pages.set(i, page.substring(0, 256));
            }
        }
    }
}
