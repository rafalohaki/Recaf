package me.coley.recaf.ui.pane;

import javafx.scene.layout.BorderPane;
import me.coley.recaf.RecafUI;
import me.coley.recaf.search.Search;
import me.coley.recaf.search.result.Result;
import me.coley.recaf.ui.control.tree.WorkspaceTree;
import me.coley.recaf.ui.control.tree.WorkspaceTreeType;
import me.coley.recaf.ui.control.tree.item.ResultsRootItem;
import me.coley.recaf.util.Threads;
import me.coley.recaf.workspace.Workspace;

import java.util.Collection;

public class ResultsPane extends BorderPane {
	private final WorkspaceTree tree = new WorkspaceTree(WorkspaceTreeType.SEARCH_RESULTS);

	public ResultsPane(Search search, Collection<Result> results) {
		tree.setShowRoot(true);
		setCenter(tree);
		// Set new root item
		Workspace workspace = RecafUI.getController().getWorkspace();
		ResultsRootItem root = new ResultsRootItem(workspace, search, results);
		root.setup();
		// Updating root must be on UI thread
		Threads.runFx(() -> {
			tree.setRoot(root);
			tree.getRoot().setExpanded(true);
			tree.getSelectionModel().select(0);
			tree.requestFocus();
		});
		// TODO: Since 'ResultsRootItem' implements the listener types, it should be registered so that if the
		//       workspace updates the results can update live.
		//       Just make sure the listener gets removed when this panel is removed / closed.
		//        - Implement Cleanable and the docking system should auto-clean it when it gets removed
	}
}