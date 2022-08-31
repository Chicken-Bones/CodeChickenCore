package codechicken.obfuscator;

import codechicken.obfuscator.ObfuscationMap.ObfuscationEntry;
import java.util.List;

public interface IHeirachyEvaluator {
    /**
     * @param desc The mapping descriptor of the class to evaluate heirachy for
     * @return A list of parents (srg or obf names)
     */
    public List<String> getParents(ObfuscationEntry desc);

    /**
     * @param desc The mapping descriptor of the class in question
     * @return True if this class does not inherit from any obfuscated class.
     */
    public boolean isLibClass(ObfuscationEntry desc);
}
