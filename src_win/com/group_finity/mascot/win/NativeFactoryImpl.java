package com.group_finity.mascot.win;

import com.group_finity.mascot.NativeFactory;
import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;

import java.awt.image.BufferedImage;

/**
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
public class NativeFactoryImpl extends NativeFactory {

    private Environment environment = new WindowsEnvironment();

    @Override
    public Environment getEnvironment() {
        return this.environment;
    }

    @Override
    public NativeImage newNativeImage(final BufferedImage src) {
        return new WindowsNativeImage(src);
    }

    @Override
    public TranslucentWindow newTransparentWindow() {
        return new WindowsTranslucentWindow();
    }

}
