/*
 * Modlauncher - utility to launch Minecraft-like game environments with runtime transformation
 * Copyright ©2016-2017 cpw and others
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.TransformationServiceDecorator;
import cpw.mods.modlauncher.TransformTargetLabel;
import cpw.mods.modlauncher.TransformList;
import cpw.mods.modlauncher.TransformStore;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.powermock.reflect.Whitebox;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransformationServiceDecoratorTests
{
    private final ClassNodeTransformer classNodeTransformer = new ClassNodeTransformer();
    private final MethodNodeTransformer methodNodeTransformer = new MethodNodeTransformer();

    @Test
    void testGatherTransformersNormally() throws Exception
    {
        MockTransformerService mockTransformerService = new MockTransformerService()
        {
            @Nonnull
            @Override
            public List<ITransformer> transformers()
            {
                return Stream.of(classNodeTransformer, methodNodeTransformer).collect(Collectors.toList());
            }
        };
        TransformStore store = new TransformStore();

        TransformationServiceDecorator sd = Whitebox.invokeConstructor(TransformationServiceDecorator.class, mockTransformerService);
        sd.gatherTransformers(store);
        EnumMap<TransformTargetLabel.LabelType, TransformList<?>> transformers = Whitebox.getInternalState(store, "transformers");
        Set<TransformTargetLabel> targettedClasses = Whitebox.getInternalState(store, "classNeedsTransforming");
        assertAll(
                () -> assertTrue(transformers.containsKey(TransformTargetLabel.LabelType.CLASS)),
                () -> assertTrue(transformers.get(TransformTargetLabel.LabelType.CLASS).getTransformers().values().stream().flatMap(Collection::stream).allMatch(s -> s == classNodeTransformer)),
                () -> assertTrue(targettedClasses.contains(new TransformTargetLabel("cheese.Puffs"))),
                () -> assertTrue(transformers.containsKey(TransformTargetLabel.LabelType.METHOD)),
                () -> assertTrue(transformers.get(TransformTargetLabel.LabelType.METHOD).getTransformers().values().stream().flatMap(Collection::stream).allMatch(s -> s == methodNodeTransformer)),
                () -> assertTrue(targettedClasses.contains(new TransformTargetLabel("cheesy.PuffMethod")))
        );
    }

    private static class ClassNodeTransformer implements ITransformer<ClassNode>
    {
        @Nonnull
        @Override
        public ClassNode transform(ClassNode input, ITransformerVotingContext context)
        {
            return input;
        }

        @Nonnull
        @Override
        public TransformerVoteResult castVote(ITransformerVotingContext context)
        {
            return TransformerVoteResult.YES;
        }

        @Nonnull
        @Override
        public Set<Target> targets()
        {
            return Stream.of(Target.targetClass("cheese.Puffs")).collect(Collectors.toSet());
        }
    }

    private static class MethodNodeTransformer implements ITransformer<MethodNode>
    {
        @Nonnull
        @Override
        public MethodNode transform(MethodNode input, ITransformerVotingContext context)
        {
            return input;
        }

        @Nonnull
        @Override
        public TransformerVoteResult castVote(ITransformerVotingContext context)
        {
            return TransformerVoteResult.YES;
        }

        @Nonnull
        @Override
        public Set<Target> targets()
        {
            return Stream.of(Target.targetMethod("cheesy.PuffMethod", "fish", "()V")).collect(Collectors.toSet());
        }
    }
}
