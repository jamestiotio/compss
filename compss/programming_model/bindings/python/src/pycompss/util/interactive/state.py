#!/usr/bin/python
#
#  Copyright 2002-2021 Barcelona Supercomputing Center (www.bsc.es)
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

# -*- coding: utf-8 -*-

"""
PyCOMPSs Interactive State
==========================
    Provides auxiliary methods for the interactive mode to get the state
"""

import os
from pycompss.util.typing_helper import typing
from collections import defaultdict
from pycompss.util.exceptions import PyCOMPSsException
from xml.etree import ElementTree

try:
    from IPython.display import HTML  # noqa
    from IPython.display import display  # noqa

    MISSING_DEPENDENCY = "tabulate"  # noqa
    import tabulate  # noqa

    MISSING_DEPENDENCY = "matplotlib"  # noqa
    import matplotlib.pyplot as plt  # noqa

    MISSING_DEPENDENCY = "numpy"  # noqa
    import numpy as np  # noqa

    np.random.seed(0)
    MISSING_DEPENDENCY = "None"  # noqa
except ImportError:
    HTML = None
    display = None
    plt = None

try:
    import ipywidgets as widgets  # noqa
except ImportError:
    widgets = None


def supports_dynamic_state():
    # type: () -> bool
    """Checks if the state can be displayed with widgets.

    :return: True if widgets available. False otherwise.
    """
    return widgets is not None


def check_monitoring_file(log_path):
    # type: (str) -> bool
    """Check if there monitoring file exists or not.

    :param log_path: Absolute path of the log folder.
    :return: True if the compss monitoring file exists. False otherwise.
    """
    compss_state_xml = get_compss_state_xml(log_path)
    return os.path.exists(compss_state_xml)


def get_compss_state_xml(log_path):
    # type: (str) -> str
    """Check if there is any missing package and return the status xml
    full path.

    :param log_path: Absolute path of the log folder.
    :return: The compss state full path.
    """
    if MISSING_DEPENDENCY != "None":
        raise PyCOMPSsException("Missing %s package." % MISSING_DEPENDENCY)
    compss_state_xml = os.path.join(log_path, "monitor", "COMPSs_state.xml")
    return compss_state_xml


def parse_state_xml(log_path, field):
    # type: (str, str) -> typing.Any
    """Converts the given xml to dictionary.

    :param log_path: Absolute path of the log folder.
    :param field: Field name to retrieve.
    :return: The content as dictionary (unless CoresInfo, which is a list)
    """
    state_xml = get_compss_state_xml(log_path)
    tree = ElementTree.parse(state_xml)
    root = tree.getroot()
    state_xml_dict = element_tree_to_dict(root)
    if field == "TasksInfo":
        return state_xml_dict["COMPSsState"][field]["Application"]
    elif field == "CoresInfo":
        return state_xml_dict["COMPSsState"][field]["Core"]  # this is a list
    elif field == "Statistics":
        return state_xml_dict["COMPSsState"][field]["Statistic"]
    elif field == "ResourceInfo":
        return state_xml_dict["COMPSsState"][field]["Resource"]
    else:
        raise PyCOMPSsException("Unsupported status field")


def element_tree_to_dict(element_tree):
    # type: (typing.Any) -> dict
    """Converts a element tree into a dictionary recursively.

    :param element_tree: Element tree.
    :return: Dictionary.
    """
    d = {element_tree.tag: {} if element_tree.attrib else None}  # type: dict
    children = list(element_tree)
    if children:
        dd = defaultdict(list)
        for dc in map(element_tree_to_dict, children):
            for k, v in dc.items():
                dd[k].append(v)
        d = {element_tree.tag: {k: v[0] if len(v) == 1 else v for k, v in dd.items()}}
    if element_tree.attrib:
        d[element_tree.tag].update(("@" + k, v) for k, v in element_tree.attrib.items())
    if element_tree.text:
        text = element_tree.text.strip()
        if children or element_tree.attrib:
            if text:
                d[element_tree.tag]["#text"] = text
        else:
            d[element_tree.tag] = text
    return d


def show_tasks_info(log_path):
    # type: (str) -> None
    """Show tasks info.

    :param log_path: Absolute path of the log folder.
    :return: None
    """
    if supports_dynamic_state():

        def f(i):  # noqa
            # type: (typing.Any) -> None
            __show_tasks_info__(log_path)

        play = __get_play_widget(f)
        display(play)  # noqa
    else:
        __show_tasks_info__(log_path)


def __show_tasks_info__(log_path):
    # type: (str) -> None
    """Show tasks info.

    :param log_path: Absolute path of the log folder.
    :return: None
    """
    cores_info = parse_state_xml(log_path, "CoresInfo")
    labels = [
        "Signature",
        "ExecutedCount",
        "MaxExecutionTime",
        "MeanExecutionTime",
        "MinExecutionTime",
    ]
    cores = []
    for core in cores_info:
        new_task = []
        for label in labels:
            if label == "Signature":
                new_task.append(core["Impl"][label].split(".")[-1])
            elif label == "ExecutedCount":
                new_task.append(int(core["Impl"][label]))
            else:
                new_task.append(int(core["Impl"][label]) / 1000)
        cores.append(new_task)
    # Display graph
    task_names = [row[0] for row in cores]
    task_names[0] = "TaskName"
    task_count = [row[1] for row in cores]
    task_max = [row[2] for row in cores]
    task_mean = [row[3] for row in cores]
    task_min = [row[4] for row in cores]
    fig = plt.figure()
    ax1 = fig.add_subplot(1, 2, 1)
    ax2 = fig.add_subplot(1, 2, 2)
    errs = [[], []]  # type: typing.List[typing.List[str]]
    for i in range(len(task_names)):
        min_ = task_mean[i] - task_min[i]
        max_ = task_max[i] - task_mean[i]
        errs[0].append(min_)
        errs[1].append(max_)
    ax1.errorbar(task_names, task_mean, yerr=errs, lw=3, fmt="ok")
    ax1.set_ylabel("Time (seconds)")
    ax1.set_xlabel("Task name")
    task_mean = [mean * 1000 for mean in task_mean]
    ax2.scatter(
        task_names,
        task_count,
        s=task_mean,
        c=np.random.rand(len(task_names)),
        alpha=0.5,
    )
    ax2.set_ylim(0, max(task_count))
    ax2.ticklabel_format(axis="y", style="plain")
    ax2.set_ylabel("Amount of tasks")
    ax2.set_xlabel("Task name")
    plt.tight_layout()
    plt.show()
    # Display table with values
    display(HTML(tabulate.tabulate(cores, tablefmt="html", headers=labels)))  # noqa


def show_tasks_status(log_path):
    # type: (str) -> None
    """Show tasks status.

    :param log_path: Absolute path of the log folder.
    :return: None
    """
    if supports_dynamic_state():

        def f(i):  # noqa
            # type: (typing.Any) -> None
            __show_tasks_status__(log_path)

        play = __get_play_widget(f)
        display(play)  # noqa
    else:
        __show_tasks_info__(log_path)


def __show_tasks_status__(log_path):
    # type: (str) -> None
    """Show tasks status.

    :param log_path: Absolute path of the log folder.
    :return: None
    """
    tasks_info_dict = parse_state_xml(log_path, "TasksInfo")
    # Display graph
    labels = ["InProgress", "Completed"]
    sizes = [tasks_info_dict[labels[0]], tasks_info_dict[labels[1]]]
    explode = (0, 0)
    fig1, ax1 = plt.subplots()
    colors = ["b", "g"]
    ax1.pie(
        sizes, explode=explode, labels=labels, colors=colors, shadow=True, startangle=90
    )
    ax1.axis("equal")
    plt.show()
    # Display table with values
    labels, values = __plain_lists__(tasks_info_dict)
    display(HTML(tabulate.tabulate([values], tablefmt="html", headers=labels)))  # noqa


def show_statistics(log_path):
    # type: (str) -> None
    """Show statistics info.

    :param log_path: Absolute path of the log folder.
    :return: None
    """
    if supports_dynamic_state():

        def f(i):  # noqa
            # type: (typing.Any) -> None
            __show_statistics__(log_path)

        play = __get_play_widget(f)
        display(play)  # noqa
    else:
        __show_statistics__(log_path)


def __show_statistics__(log_path):
    # type: (str) -> None
    """Show statistics info.

    :param log_path: Absolute path of the log folder.
    :return: None
    """
    statistics_dict = parse_state_xml(log_path, "Statistics")
    # Display table with values
    labels = [statistics_dict["Key"]]
    values = [statistics_dict["Value"]]
    display(HTML(tabulate.tabulate([values], tablefmt="html", headers=labels)))  # noqa


def show_resources_status(log_path):
    # type: (str) -> None
    """Show resources status info.

    :param log_path: Absolute path of the log folder.
    :return: None
    """
    if supports_dynamic_state():

        def f(i):  # noqa
            # type: (typing.Any) -> None
            __show_resources_status__(log_path)

        play = __get_play_widget(f)
        display(play)  # noqa
    else:
        __show_resources_status__(log_path)


def __show_resources_status__(log_path):
    # type: (str) -> None
    """Show resources status info.

    :param log_path: Absolute path of the log folder.
    :return: None
    """
    resource_info_dict = parse_state_xml(log_path, "ResourceInfo")
    # Display table with values
    labels, values = __plain_lists__(resource_info_dict)
    display(HTML(tabulate.tabulate([values], tablefmt="html", headers=labels)))  # noqa


def __plain_lists__(dictionary):
    # type: (dict) -> typing.Tuple[list, list]
    """Converts a dictionary to two lists.
    Removes last element.

    :param dictionary: Dictionary to plain.
    :return: Labels and values
    """
    labels = []
    values = []
    for k, v in dictionary.items():
        labels.append(k)
        values.append(v)
    labels.pop()
    values.pop()
    return labels, values


def __get_play_widget(function):
    # type: (typing.Any) -> typing.Any
    """Generate play widget.

    :param function: Function to associate with Play.
    :return: Play widget.
    """
    play = widgets.interactive(
        function,
        i=widgets.Play(
            value=0,
            min=0,
            max=500,
            step=1,
            interval=5000,
            description="Press play",
            disabled=False,
        ),
    )
    return play
