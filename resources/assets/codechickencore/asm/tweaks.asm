#A bunch of vanilla tweaks

list d_environmentallyFriendlyCreepers
ALOAD 0
GETFIELD net/minecraft/entity/monster/EntityCreeper.field_70170_p:Lnet/minecraft/world/World; #worldObj
INVOKEVIRTUAL net/minecraft/world/World.func_82736_K()Lnet/minecraft/world/GameRules; #getGameRules
LDC "mobGriefing"
INVOKEVIRTUAL net/minecraft/world/GameRules.func_82766_b(Ljava/lang/String;)Z #getGameRuleBooleanValue

list environmentallyFriendlyCreepers
ICONST_0

list softLeafReplace
ALOAD 0
ALOAD 1
ILOAD 2
ILOAD 3
ILOAD 4
INVOKEVIRTUAL net/minecraft/block/Block.isAir(Lnet/minecraft/world/IBlockAccess;III)Z #forge method
IRETURN

list n_doFireTick
LDC "doFireTick"
INVOKEVIRTUAL net/minecraft/world/GameRules.func_82766_b(Ljava/lang/String;)Z #getGameRuleBooleanValue
IFEQ LRET

list doFireTick
ALOAD 1
ILOAD 2
ILOAD 3
ILOAD 4
ALOAD 5
INVOKESTATIC codechicken/core/featurehack/TweakTransformerHelper.quenchFireTick(Lnet/minecraft/world/World;IIILjava/util/Random;)V
LSKIP

list finiteWater
ALOAD 0
GETFIELD net/minecraft/block/BlockDynamicLiquid.field_149815_a:I
ICONST_2
IF_ICMPLT LEND