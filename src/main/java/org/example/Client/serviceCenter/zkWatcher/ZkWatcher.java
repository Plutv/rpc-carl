package org.example.Client.serviceCenter.zkWatcher;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.example.Client.cache.ServiceCache;

public class ZkWatcher {
    private CuratorFramework client;

    private ServiceCache cache;

    public ZkWatcher(CuratorFramework client, ServiceCache cache) {
        this.client = client;
        this.cache = cache;
    }

    public void watchToUpdate(String path) throws InterruptedException{
        CuratorCache curatorCache = CuratorCache.build(client, "/");
        curatorCache.listenable().addListener(new CuratorCacheListener() {
            @Override
            public void event(Type type, ChildData childData, ChildData childData1) {
                switch (type) {
                    case NODE_CREATED:
                        String[] pathList = parsePath(childData1);
                        if (pathList.length <= 2) break;
                        else {
                            String serviceName = pathList[1];
                            String address = pathList[2];
                            cache.addServiceToCache(serviceName, address);
                        }
                        break;
                    case NODE_CHANGED:
                        if (childData.getData() != null) {
                            System.out.println("change before: " + childData.getData());
                        } else {
                            System.out.println("first assignment.");
                        }
                        // TODO: 判断 null
                        String[] oldPathList = parsePath(childData);
                        String[] newPathList = parsePath(childData1);
                        cache.replaceServiceAddress(oldPathList[1], oldPathList[2], newPathList[2]);
                        System.out.println("change after: " + childData1.getData());
                        break;
                    case NODE_DELETED:
                        String[] pathListDel = parsePath(childData);
                        cache.delete(pathListDel[1], pathListDel[2]);
                        break;
                    default:
                        break;
                }
            }
        });
        curatorCache.start();
    }

    private String[] parsePath(ChildData childData) {
        String path = new String(childData.getData());
        return path.split("/");
    }
}
