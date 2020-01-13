/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.document;

import java.io.Closeable;
import java.io.IOException;

import net.sourceforge.pmd.util.document.TextRegion.RegionWithLines;
import net.sourceforge.pmd.util.document.io.VirtualFile;

/**
 * Represents a textual document, providing methods to edit it incrementally
 * and address regions of text. A text document delegates IO operations
 * to a {@link VirtualFile}. It reflects some snapshot of the file,
 * though the file may still be edited externally. We do not poll for
 * external modifications, instead {@link VirtualFile} provides a
 * very simple stamping system to avoid overwriting external modifications
 * (by failing in {@link TextEditor#close()}).
 */
public interface TextDocument extends Closeable {


    /**
     * Returns the current text of this document. Note that this can only
     * be updated through {@link #newEditor()} and that this doesn't take
     * external modifications to the {@link VirtualFile} into account.
     */
    CharSequence getText();


    /**
     * Returns the length in characters of the {@linkplain #getText() text}.
     */
    int getLength();


    /**
     * Create a new region based on its start offset and length. The
     * parameters must identify a valid region in the document. Valid
     * start offsets range from 0 to {@link #getLength()} (inclusive).
     * The sum {@code startOffset + length} must range from {@code startOffset}
     * to {@link #getLength()} (inclusive).
     *
     * <p>Those rules make the region starting at {@link #getLength()}
     * with length 0 a valid region (the caret position at the end of the document).
     *
     * <p>For example, for a document of length 1 ({@code "c"}), there
     * are only three valid regions:
     * <pre>{@code
     * [[c     : caret position at offset 0 (empty region)
     *  [c[    : range containing the character
     *   c[[   : caret position at offset 1 (empty region)
     * }</pre>
     *
     * @param startOffset 0-based, inclusive offset for the start of the region
     * @param length      Length of the region in characters.
     *
     * @throws InvalidRegionException If the arguments do not identify
     *                                a valid region in this document
     */
    TextRegion createRegion(int startOffset, int length);


    /**
     * Turn a text region into a {@link RegionWithLines}. If the region
     * is already a {@link RegionWithLines}, that information is discarded.
     *
     * @return A new region with line information
     *
     * @throws InvalidRegionException If the argument is not a valid region in this document
     */
    RegionWithLines addLineInfo(TextRegion region);


    /**
     * Returns a region of the {@linkplain #getText() text} as a character sequence.
     */
    CharSequence subSequence(TextRegion region);


    /**
     * Returns true if this document cannot be edited. In that case,
     * {@link #newEditor()} will throw an exception. In the general case,
     * nothing prevents this method's result from changing from one
     * invocation to another.
     */
    boolean isReadOnly();


    /**
     * Produce a new editor to edit this file. This is like calling
     * {@link #newEditor(EditorCommitHandler)} with a commit handler
     * that writes contents to the backend file.
     *
     * @return A new editor
     *
     * @see #newEditor(EditorCommitHandler)
     */
    default TextEditor newEditor() throws IOException {
        return newEditor(VirtualFile::writeContents);
    }


    /**
     * Produce a new editor to edit this file. An editor records modifications
     * and finally commits them with {@link TextEditor#close() close}. After the
     * {@code close} method is called, the commit handler parameter is called with
     * this file's backend as parameter, and the {@linkplain #getText() text} of this
     * document is updated. That may render existing text regions created by this
     * document invalid (they won't address the same text, or could be out-of-bounds).
     * Before then, all text regions created by this document stay valid, even after
     * some updates.
     *
     * <p>Only a single editor may be open at a time.
     *
     * @param handler Handles closing of the {@link TextEditor}.
     *                {@link EditorCommitHandler#commitNewContents(VirtualFile, CharSequence) commitNewContents}
     *                is called with the backend file and the new text
     *                as parameters.
     *
     * @return A new editor
     *
     * @throws IOException                     If an IO error occurs
     * @throws IOException                     If this document was closed
     * @throws ReadOnlyFileException           If this document is read-only
     * @throws ConcurrentModificationException If an editor is already open for this document
     */
    TextEditor newEditor(EditorCommitHandler handler) throws IOException;


    /**
     * Closing a document closes the underlying {@link VirtualFile}.
     * New editors cannot be produced after that, and the document otherwise
     * remains in its current state.
     *
     * @throws IOException           If {@link VirtualFile#close()} throws
     * @throws IllegalStateException If an editor is currently open. In this case
     *                               the editor is rendered ineffective before the
     *                               exception is thrown. This indicates a programming
     *                               mistake.
     */
    @Override
    void close() throws IOException;


    /**
     * Returns a document backed by the given text "file".
     *
     * @throws IOException If an error occurs eg while reading the file contents
     */
    static TextDocument create(VirtualFile virtualFile) throws IOException {
        return new TextDocumentImpl(virtualFile);
    }


    /**
     * Returns a read-only document for the given text.
     */
    static TextDocument readOnlyString(final String source) {
        try {
            return new TextDocumentImpl(VirtualFile.readOnlyString(source));
        } catch (IOException e) {
            throw new AssertionError("ReadonlyStringBehavior should never throw IOException", e);
        }
    }


    interface EditorCommitHandler {


        /**
         * Commits the edited contents of the file.
         *
         * @param originalFile File backing the {@link TextDocument}
         * @param newContents  New contents of the file
         *
         * @throws IOException If an I/O error occurs
         */
        void commitNewContents(VirtualFile originalFile, CharSequence newContents) throws IOException;

    }

}
