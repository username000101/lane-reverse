package defpackage;

import android.content.Intent;
import android.util.Log;
import androidx.compose.animation.core.AnimationScope;
import androidx.compose.animation.core.TweenSpec;
import androidx.compose.foundation.BasicTooltipState;
import androidx.compose.foundation.draganddrop.DragAndDropStartDetectorScope;
import androidx.compose.foundation.gestures.Drag2DScope;
import androidx.compose.foundation.gestures.DragEvent;
import androidx.compose.foundation.layout.CrossAxisAlignment;
import androidx.compose.foundation.layout.FlowLayoutKt;
import androidx.compose.foundation.layout.PaddingValuesImpl;
import androidx.compose.foundation.text.AndroidCursorHandle_androidKt;
import androidx.compose.foundation.text.Handle;
import androidx.compose.foundation.text.contextmenu.provider.BasicTextContextMenuProvider;
import androidx.compose.foundation.text.selection.OffsetProvider;
import androidx.compose.foundation.text.selection.SelectionHandleAnchor;
import androidx.compose.foundation.text.selection.SelectionHandleInfo;
import androidx.compose.foundation.text.selection.SelectionHandlesKt;
import androidx.compose.material.BackdropScaffoldState;
import androidx.compose.material.BottomDrawerState;
import androidx.compose.material.DrawerKt;
import androidx.compose.material3.AppBarKt;
import androidx.compose.material3.BottomAppBarScrollBehavior;
import androidx.compose.material3.BottomAppBarState;
import androidx.compose.material3.DatePickerKt;
import androidx.compose.material3.SingleRowTopAppBarOverrideScope;
import androidx.compose.material3.TooltipState;
import androidx.compose.material3.TopAppBarState;
import androidx.compose.material3.TwoRowsTopAppBarOverrideScope;
import androidx.compose.runtime.DisposableEffectResult;
import androidx.compose.runtime.DisposableEffectScope;
import androidx.compose.runtime.SnapshotMutableStateImpl;
import androidx.compose.runtime.collection.MutableVector;
import androidx.compose.ui.geometry.Offset;
import androidx.compose.ui.input.pointer.PointerEventKt;
import androidx.compose.ui.input.pointer.PointerInputChange;
import androidx.compose.ui.layout.MeasureResult;
import androidx.compose.ui.semantics.SemanticsActions;
import androidx.compose.ui.semantics.SemanticsPropertiesKt;
import androidx.compose.ui.semantics.SemanticsPropertyKey;
import androidx.compose.ui.semantics.SemanticsPropertyReceiver;
import androidx.compose.ui.text.font.FontFamilyResolverImpl;
import androidx.compose.ui.text.font.TypefaceRequest;
import androidx.compose.ui.text.input.BackspaceCommand;
import androidx.compose.ui.text.input.CommitTextCommand;
import androidx.compose.ui.text.input.DeleteAllCommand;
import androidx.compose.ui.text.input.DeleteSurroundingTextCommand;
import androidx.compose.ui.text.input.DeleteSurroundingTextInCodePointsCommand;
import androidx.compose.ui.text.input.EditCommand;
import androidx.compose.ui.text.input.FinishComposingTextCommand;
import androidx.compose.ui.text.input.MoveCursorCommand;
import androidx.compose.ui.text.input.SetComposingRegionCommand;
import androidx.compose.ui.text.input.SetComposingTextCommand;
import androidx.compose.ui.text.input.SetSelectionCommand;
import androidx.compose.ui.unit.IntOffset;
import androidx.datastore.core.CorruptionException;
import androidx.sqlite.SQLiteConnection;
import coil3.disk.DiskLruCache;
import com.google.firebase.sessions.SessionData;
import com.google.firebase.sessions.SessionDataSerializer;
import com.google.firebase.sessions.Time;
import com.skiy.lane.DeepLinkActivity;
import com.skiy.lane.MainActivity;
import com.skiy.lane.app.playback.MusicPlayerController;
import com.skiy.lane.domain.model.LaneOpenShareItem;
import com.skiy.lane.domain.model.TrackData;
import com.skiy.lane.presentation.compose.comments.CommentsViewModel;
import com.skiy.lane.presentation.compose.main.MainViewModel;
import com.skiy.lane.presentation.compose.upload.ImageUploadViewModel;
import com.skiy.lane.presentation.compose.upload.UploadState;
import dev.romainguy.kotlin.math.Float3;
import io.github.sceneview.node.CameraNode;
import io.github.sceneview.node.Node;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.collections.AbstractCollection;
import kotlin.collections.AbstractMap;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Reflection;
import kotlin.math.MathKt;
import kotlin.ranges.IntProgression;
import kotlin.ranges.RangesKt;
import kotlin.reflect.KProperty;
import kotlinx.coroutines.flow.MutableStateFlow;
import okhttp3.HttpUrl;
import okio.ForwardingFileSystem;
import okio.Path;

/* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class c implements Function1 {
    public final /* synthetic */ int a;
    public final /* synthetic */ Object b;

    public /* synthetic */ c(Object obj, int i) {
        this.a = i;
        this.b = obj;
    }

    @Override // kotlin.jvm.functions.Function1
    public final Object invoke(Object obj) {
        String strConcat;
        StringBuilder sb;
        int i;
        int i2 = this.a;
        String str = null;
        Unit unit = Unit.a;
        Object obj2 = this.b;
        switch (i2) {
            case 0:
                return obj == ((AbstractCollection) obj2) ? "(this Collection)" : String.valueOf(obj);
            case 1:
                AbstractMap abstractMap = (AbstractMap) obj2;
                Map.Entry entry = (Map.Entry) obj;
                entry.getClass();
                StringBuilder sb2 = new StringBuilder();
                Object key = entry.getKey();
                sb2.append(key == abstractMap ? "(this Map)" : String.valueOf(key));
                sb2.append('=');
                Object value = entry.getValue();
                sb2.append(value != abstractMap ? String.valueOf(value) : "(this Map)");
                return sb2.toString();
            case 2:
                float f = AndroidCursorHandle_androidKt.a;
                ((SemanticsPropertyReceiver) obj).b(SelectionHandlesKt.a, new SelectionHandleInfo(Handle.a, ((OffsetProvider) obj2).a(), SelectionHandleAnchor.b, true));
                return unit;
            case 3:
                float fFloatValue = ((Float) obj).floatValue();
                float f2 = AppBarKt.a;
                BottomAppBarState state = ((BottomAppBarScrollBehavior) obj2).getState();
                state.f(state.b() - fFloatValue);
                return unit;
            case 4:
                float f3 = AppBarKt.a;
                ((BottomAppBarState) obj2).f(((Number) ((SnapshotMutableStateImpl) ((AnimationScope) obj).e).getA()).floatValue());
                return unit;
            case 5:
                HttpUrl httpUrl = (HttpUrl) obj2;
                String str2 = (String) obj;
                str2.getClass();
                httpUrl.getClass();
                List list = httpUrl.g;
                if (list != null) {
                    IntProgression intProgressionI = RangesKt.i(RangesKt.j(0, list.size()), 2);
                    int i3 = intProgressionI.a;
                    int i4 = intProgressionI.b;
                    int i5 = intProgressionI.c;
                    if ((i5 > 0 && i3 <= i4) || (i5 < 0 && i4 <= i3)) {
                        while (!str2.equals(list.get(i3))) {
                            if (i3 != i4) {
                                i3 += i5;
                            }
                        }
                        str = (String) list.get(i3 + 1);
                    }
                }
                return w.C(str2, "=", str);
            case 6:
                ((Float) obj).getClass();
                return Float.valueOf(((BackdropScaffoldState) obj2).a().z1(56.0f));
            case 7:
                CameraNode cameraNode = (CameraNode) obj;
                cameraNode.getClass();
                cameraNode.H(new Float3(0.0f, 0.4f, 2.5f));
                Node.p(cameraNode, (Node) obj2);
                return unit;
            case 8:
                final BasicTextContextMenuProvider basicTextContextMenuProvider = (BasicTextContextMenuProvider) obj2;
                return new DisposableEffectResult() { // from class: androidx.compose.foundation.text.contextmenu.provider.BasicTextContextMenuProviderKt$basicTextContextMenuProvider$lambda$1$0$$inlined$onDispose$1
                    @Override // androidx.compose.runtime.DisposableEffectResult
                    public final void dispose() {
                        BasicTextContextMenuProvider.SessionImpl sessionImpl = (BasicTextContextMenuProvider.SessionImpl) ((SnapshotMutableStateImpl) basicTextContextMenuProvider.c).getA();
                        if (sessionImpl != null) {
                            sessionImpl.close();
                        }
                    }
                };
            case 9:
                final TooltipState tooltipState = (TooltipState) obj2;
                return new DisposableEffectResult() { // from class: androidx.compose.material3.internal.BasicTooltipKt$BasicTooltipBox$lambda$4$lambda$3$$inlined$onDispose$1
                    @Override // androidx.compose.runtime.DisposableEffectResult
                    public final void dispose() {
                        tooltipState.a();
                    }
                };
            case 10:
                final BasicTooltipState basicTooltipState = (BasicTooltipState) obj2;
                return new DisposableEffectResult() { // from class: androidx.compose.foundation.BasicTooltipKt$BasicTooltipBox$lambda$1$0$$inlined$onDispose$1
                    @Override // androidx.compose.runtime.DisposableEffectResult
                    public final void dispose() {
                        basicTooltipState.a();
                    }
                };
            case 11:
                final CommentsViewModel commentsViewModel = (CommentsViewModel) obj2;
                ((DisposableEffectScope) obj).getClass();
                return new DisposableEffectResult() { // from class: com.skiy.lane.presentation.compose.comments.CommentsSheetContentKt$CommentsSheetContent$lambda$5$0$$inlined$onDispose$1
                    @Override // androidx.compose.runtime.DisposableEffectResult
                    public final void dispose() {
                        Object value2;
                        MutableStateFlow mutableStateFlow = commentsViewModel.f;
                        do {
                            value2 = mutableStateFlow.getValue();
                        } while (!mutableStateFlow.e(value2, new CommentsUiState(((CommentsUiState) value2).i, 32255)));
                    }
                };
            case 12:
                ((SQLiteConnection) obj).getClass();
                return ((Callable) obj2).call();
            case 13:
                PaddingValuesImpl paddingValuesImpl = DatePickerKt.a;
                SemanticsPropertiesKt.k((SemanticsPropertyReceiver) obj, (String) ((Pair) obj2).a);
                return unit;
            case 14:
                KProperty[] kPropertyArr = SemanticsPropertiesKt.a;
                SemanticsPropertyKey semanticsPropertyKey = SemanticsActions.x;
                KProperty kProperty = SemanticsPropertiesKt.a[29];
                ((SemanticsPropertyReceiver) obj).b(semanticsPropertyKey, (List) obj2);
                return unit;
            case 15:
                DeepLinkActivity deepLinkActivity = (DeepLinkActivity) obj2;
                LaneOpenShareItem laneOpenShareItem = (LaneOpenShareItem) obj;
                int i6 = DeepLinkActivity.F;
                laneOpenShareItem.getClass();
                Intent intent = new Intent(deepLinkActivity, (Class<?>) MainActivity.class);
                intent.setFlags(335544320);
                intent.putExtra("deep_link_type", laneOpenShareItem.getShareContentType());
                intent.putExtra("deep_link_id", laneOpenShareItem.getShareItemId());
                deepLinkActivity.startActivity(intent);
                deepLinkActivity.finish();
                return unit;
            case 16:
                float fFloatValue2 = ((Float) obj).floatValue();
                TopAppBarState state2 = ((SingleRowTopAppBarOverrideScope) obj2).l.getState();
                state2.d(state2.b() + fFloatValue2);
                return unit;
            case 17:
                float fFloatValue3 = ((Float) obj).floatValue();
                TopAppBarState state3 = ((TwoRowsTopAppBarOverrideScope) obj2).r.getState();
                state3.d(state3.b() + fFloatValue3);
                return unit;
            case 18:
                return DiskLruCache.newJournalWriter$lambda$0((DiskLruCache) obj2, (IOException) obj);
            case 19:
                ((DragAndDropStartDetectorScope) obj2).g(((Offset) obj).a);
                return unit;
            case 20:
                PointerInputChange pointerInputChange = (PointerInputChange) obj;
                ((u6) obj2).invoke(pointerInputChange, Float.valueOf(Float.intBitsToFloat((int) (PointerEventKt.f(pointerInputChange, false) & 4294967295L))));
                pointerInputChange.a();
                return unit;
            case 21:
                ((Drag2DScope) obj2).a(((DragEvent.DragDelta) obj).a);
                return unit;
            case 22:
                TweenSpec tweenSpec = DrawerKt.a;
                return new IntOffset(((long) MathKt.b(((BottomDrawerState) obj2).a.g())) & 4294967295L);
            case 23:
                EditCommand editCommand = (EditCommand) obj;
                String str3 = ((EditCommand) obj2) == editCommand ? " > " : "   ";
                if (!(editCommand instanceof CommitTextCommand)) {
                    if (editCommand instanceof SetComposingTextCommand) {
                        sb = new StringBuilder("SetComposingTextCommand(text.length=");
                        SetComposingTextCommand setComposingTextCommand = (SetComposingTextCommand) editCommand;
                        sb.append(setComposingTextCommand.a.b.length());
                        sb.append(", newCursorPosition=");
                        i = setComposingTextCommand.b;
                    } else if (editCommand instanceof SetComposingRegionCommand) {
                        strConcat = ((SetComposingRegionCommand) editCommand).toString();
                    } else if (editCommand instanceof DeleteSurroundingTextCommand) {
                        strConcat = ((DeleteSurroundingTextCommand) editCommand).toString();
                    } else if (editCommand instanceof DeleteSurroundingTextInCodePointsCommand) {
                        strConcat = ((DeleteSurroundingTextInCodePointsCommand) editCommand).toString();
                    } else if (editCommand instanceof SetSelectionCommand) {
                        strConcat = ((SetSelectionCommand) editCommand).toString();
                    } else if (editCommand instanceof FinishComposingTextCommand) {
                        strConcat = "FinishComposingTextCommand()";
                    } else if (editCommand instanceof BackspaceCommand) {
                        strConcat = "BackspaceCommand()";
                    } else if (editCommand instanceof MoveCursorCommand) {
                        strConcat = "MoveCursorCommand(amount=0)";
                    } else if (editCommand instanceof DeleteAllCommand) {
                        strConcat = "DeleteAllCommand()";
                    } else {
                        String strG = Reflection.a(editCommand.getClass()).g();
                        if (strG == null) {
                            strG = "{anonymous EditCommand}";
                        }
                        strConcat = "Unknown EditCommand: ".concat(strG);
                    }
                    return str3.concat(strConcat);
                }
                sb = new StringBuilder("CommitTextCommand(text.length=");
                CommitTextCommand commitTextCommand = (CommitTextCommand) editCommand;
                sb.append(commitTextCommand.a.b.length());
                sb.append(", newCursorPosition=");
                i = commitTextCommand.b;
                strConcat = ih.j(sb, i, ')');
                return str3.concat(strConcat);
            case 24:
                final ImageUploadViewModel imageUploadViewModel = (ImageUploadViewModel) obj2;
                ((DisposableEffectScope) obj).getClass();
                return new DisposableEffectResult() { // from class: com.skiy.lane.presentation.compose.profile.EditProfileScreenKt$EditProfileScreen$lambda$21$0$$inlined$onDispose$1
                    @Override // androidx.compose.runtime.DisposableEffectResult
                    public final void dispose() {
                        imageUploadViewModel.c.setValue(UploadState.Idle.a);
                    }
                };
            case 25:
                CorruptionException corruptionException = (CorruptionException) obj;
                corruptionException.getClass();
                Log.w("FirebaseSessions", "CorruptionException in session data DataStore", corruptionException);
                return new SessionData(((SessionDataSerializer) obj2).a.a(null), (Time) null, (Map) null, 6, (DefaultConstructorMarker) null);
            case 26:
                MutableVector mutableVector = (MutableVector) obj2;
                CrossAxisAlignment crossAxisAlignment = FlowLayoutKt.a;
                Object[] objArr = mutableVector.a;
                int i7 = mutableVector.c;
                for (int i8 = 0; i8 < i7; i8++) {
                    ((MeasureResult) objArr[i8]).v();
                }
                return unit;
            case 27:
                TypefaceRequest typefaceRequest = (TypefaceRequest) obj;
                return ((FontFamilyResolverImpl) obj2).b(new TypefaceRequest(null, typefaceRequest.b, typefaceRequest.c, typefaceRequest.d, typefaceRequest.e)).getA();
            case 28:
                Path path = (Path) obj;
                int i9 = ForwardingFileSystem.a;
                path.getClass();
                return ((ForwardingFileSystem) obj2).onPathResult(path, "listRecursively");
            default:
                int iIntValue = ((Integer) obj).intValue();
                MusicPlayerController musicPlayerController = ((MainViewModel) obj2).d;
                TrackData trackData = (TrackData) CollectionsKt.C(iIntValue, (List) musicPlayerController.l.getValue());
                if (trackData != null) {
                    musicPlayerController.c(new oo(trackData, 16));
                }
                return unit;
        }
    }

    public /* synthetic */ c(Object obj, Object obj2, int i) {
        this.a = i;
        this.b = obj;
    }
}
