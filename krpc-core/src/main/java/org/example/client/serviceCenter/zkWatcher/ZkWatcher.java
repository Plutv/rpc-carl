package org.example.client.serviceCenter.zkWatcher;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;

public class ZkWatcher {
    private CuratorFramework client;

    private final ServiceChangeListener listener;

    public ZkWatcher(CuratorFramework client, ServiceChangeListener listener) {
        this.client = client;
        this.listener = listener;
    }

    public void watchToUpdate(String path) throws InterruptedException{
        CuratorCache curatorCache = CuratorCache.build(client, "/");
        curatorCache.listenable().addListener(new CuratorCacheListener() {
            @Override
            public void event(Type type, ChildData childData, ChildData childData1) {
                switch (type) {
                    case NODE_CREATED: {
                        String[] pathList = parsePath(childData1);
                        if (pathList.length > 2) {
                            String serviceName = pathList[1];
                            String address = pathList[2];
                            listener.onAdd(serviceName, address);
                        }
                        break;
                    }
                    case NODE_CHANGED: {
                        String[] oldPathList = parsePath(childData);
                        String[] newPathList = parsePath(childData1);
                        if (oldPathList.length > 2 && newPathList.length > 2) {
                            listener.onReplace(oldPathList[1], oldPathList[2], newPathList[2]);
                        }
                        break;
                    }
                    case NODE_DELETED: {
                        String[] pathListDel = parsePath(childData);
                        if (pathListDel.length > 2) {
                            listener.onRemove(pathListDel[1], pathListDel[2]);
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
        });
        curatorCache.start();
    }

    private String[] parsePath(ChildData childData) {
        if (childData == null || childData.getPath() == null) {
            return new String[0];
        }
        // curator 的 path 形如: /ServiceName/host:port
        return childData.getPath().split("/");
    }
}
