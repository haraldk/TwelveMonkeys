package com.twelvemonkeys.imageio.metadata.ioca;

import com.twelvemonkeys.imageio.metadata.AbstractCompoundDirectory;
import com.twelvemonkeys.imageio.metadata.Directory;

import java.util.Collection;

public final class IOCADirectory extends AbstractCompoundDirectory {

	IOCADirectory(final Collection<? extends Directory> directories) {
		super(directories);
	}
}
