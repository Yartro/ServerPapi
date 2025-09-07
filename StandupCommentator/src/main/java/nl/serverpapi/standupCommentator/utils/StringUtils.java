package nl.serverpapi.standupCommentator.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class StringUtils {
    private StringUtils(){
    }

    public static String componentToString(Component component) {
        if (component != null)
            return LegacyComponentSerializer.legacySection()
                    .serialize(component);

        return "";
    }
}