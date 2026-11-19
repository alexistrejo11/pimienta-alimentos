package io.github.alexistrejo11.pimienta.module.files.core.application;

/** Bytes ready to store, after an image has been rewritten as WebP when applicable. */
public record NormalizedFile(byte[] content, String contentType, String extension) {}
